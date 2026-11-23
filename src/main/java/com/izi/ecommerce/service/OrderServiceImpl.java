package com.izi.ecommerce.service;

import com.izi.ecommerce.common.OrderStateTransition;
import com.izi.ecommerce.common.errors.InventoryException;
import com.izi.ecommerce.entity.CartItem;
import com.izi.ecommerce.entity.Order;
import com.izi.ecommerce.entity.OrderItem;
import com.izi.ecommerce.entity.Product;
import com.izi.ecommerce.entity.UserAddress;
import com.izi.ecommerce.common.errors.ResourceNotFoundException;
import com.izi.ecommerce.model.*;
import com.izi.ecommerce.repository.CartItemRepository;
import com.izi.ecommerce.repository.OrderItemRepository;
import com.izi.ecommerce.repository.OrderRepository;
import com.izi.ecommerce.repository.ProductRepository;
import com.izi.ecommerce.repository.UserAddressRepository;
import com.xendit.exception.XenditException;
import com.xendit.model.Invoice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserAddressRepository userAddressRepository;
    private final ProductRepository productRepository;
    private final ShippingService shippingService;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;

    private static final BigDecimal TAX_RATE = BigDecimal.valueOf(0.03);

    @Override
    @Transactional
    public OrderResponse checkout(CheckoutRequest checkoutRequest) {

        Long userId = checkoutRequest.getUserId();
        Set<Long> selectedCartItemIds = new HashSet<>(checkoutRequest.getSelectedCartItemIds());

        // Only items from the current user's own cart can be checked out.
        List<CartItem> selectedItems = cartItemRepository.getUserCartItems(userId).stream()
                .filter(cartItem -> selectedCartItemIds.contains(cartItem.getCartItemId()))
                .toList();

        if (selectedItems.isEmpty() || selectedItems.size() != selectedCartItemIds.size()) {
            throw new ResourceNotFoundException("No cart items found for checkout");
        }

        UserAddress shippingAddress = userAddressRepository.findById(
                checkoutRequest.getUserAddressId()
        ).filter(address -> userId.equals(address.getUserId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                "Shipping address with id " + checkoutRequest.getUserAddressId() + " is not found"
        ));

        Map<Long,Integer> productQuantities = selectedItems.stream()
                .collect(Collectors.toMap(CartItem::getProductId, CartItem::getQuantity));

        if(!inventoryService.checkAndLockInventory(productQuantities)){
            throw new InventoryException("Inventory locked");
        }

        Order newOrder = Order.builder()
                .userId(checkoutRequest.getUserId())
                .status(OrderStatus.PENDING)
                .orderDate(LocalDateTime.now())
                .totalAmount(BigDecimal.ZERO)
                .taxFee(BigDecimal.ZERO)
                .subtotal(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .build();

        Order savedOrder = orderRepository.save(newOrder);

        List<OrderItem> orderItems = selectedItems.stream()
                .map(cartItem -> {
                    return OrderItem.builder()
                            .orderId(savedOrder.getOrderId())
                            .productId(cartItem.getProductId())
                            .quantity(cartItem.getQuantity())
                            .price(cartItem.getPrice())
                            .userAddressId(shippingAddress.getUserAddressId())
                            .build();
                })
                .toList();

        orderItemRepository.saveAll(orderItems);

        cartItemRepository.deleteAll(selectedItems);

        BigDecimal subtotal = orderItems.stream()
                .map(orderItem -> orderItem.getPrice()
                        .multiply(BigDecimal.valueOf(orderItem.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shippingFee = orderItems.stream()
                .map(orderItem -> {
                    Optional<Product> product = productRepository.findById(orderItem.getProductId());
                    if (product.isEmpty()) {
                        return BigDecimal.ZERO;
                    }

                    Optional<UserAddress> sellerAddress = userAddressRepository.findByUserIdAndIsDefaultTrue(
                            product.get()
                                    .getUserId());
                    if (sellerAddress.isEmpty()) {
                        return BigDecimal.ZERO;
                    }

                    BigDecimal totalWeight = product.get().getWeight()
                            .multiply(BigDecimal.valueOf(orderItem.getQuantity()));

                    ShippingRateRequest rateRequest = ShippingRateRequest.builder()
                            .totalWeightInGrams(totalWeight)
                            .fromAddress(ShippingRateRequest.fromUserAddress(sellerAddress.get()))
                            .toAddress(ShippingRateRequest.fromUserAddress(shippingAddress))
                            .build();
                    ShippingRateResponse rateResponse = shippingService.calculateShippingRate(rateRequest);
                    return rateResponse.getShippingFee();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal taxFee = subtotal.multiply(TAX_RATE);
        BigDecimal totalAmount = subtotal.add(taxFee).add(shippingFee);

        savedOrder.setSubtotal(subtotal);
        savedOrder.setShippingFee(shippingFee);
        savedOrder.setTaxFee(taxFee);
        savedOrder.setTotalAmount(totalAmount);

        orderRepository.save(savedOrder);
        //Generate payment url with xendit api


        String paymentUrl;

        try {
            PaymentResponse paymentResponse = paymentService.create(savedOrder);
            savedOrder.setXenditInvoiceId(paymentResponse.getXenditInvoiceId());
            savedOrder.setXenditPaymentStatus(paymentResponse.getXenditInvoiceStatus());
            paymentUrl = paymentResponse.getXenditPaymentUrl();

            orderRepository.save(savedOrder);
            inventoryService.decreaseQuantity(productQuantities);

        } catch (Exception e) {
            log.error("Payment creation for order: " + savedOrder.getOrderId()+" is failed Reason: "
                    + e.getMessage());
            savedOrder.setStatus(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(savedOrder);
            return OrderResponse.fromOrder(savedOrder);
        }


        OrderResponse orderResponse = OrderResponse.fromOrder(savedOrder);
        orderResponse.setPaymentUrl(paymentUrl);
        return orderResponse ;
    }

    @Override
    public Optional<Order> findOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    @Override
    public List<Order> findOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    public List<Order> findOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order with id " + orderId + " not found"
                ));
        if(!OrderStateTransition .isValidTransition(order.getStatus(),OrderStatus.CANCELLED)){
            throw new IllegalStateException("Only PENDING orders can be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        cancelXenditInvoice(order);
        restoreInventoryForOrder(orderId);
    }

    @Override
    public List<OrderItemResponse> findOrderItemsByOrderId(Long orderId) {

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        if (orderItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> productIds = orderItems.stream()
                .map(OrderItem::getProductId)
                .toList();

        List<Long> shippingAddressIds = orderItems.stream()
                .map(OrderItem::getUserAddressId)
                .toList();

        List<Product> products = productRepository.findAllById(productIds);

        List<UserAddress> shippingAddresses = userAddressRepository.findAllById(shippingAddressIds);

        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));

        Map<Long, UserAddress> userAddressMap = shippingAddresses.stream()
                .collect(Collectors.toMap(UserAddress::getUserAddressId, Function.identity()));

        return orderItems.stream()
                .map(orderItem -> {
                    Product product = productMap.get(orderItem.getProductId());
                    UserAddress userAddress = userAddressMap.get(orderItem.getUserAddressId());

                    if (product == null) {
                        throw new ResourceNotFoundException(
                                "Product with id " + orderItem.getProductId() + " is not found"
                        );
                    }

                    if (userAddress == null) {
                        throw new ResourceNotFoundException(
                                "User address with id " + orderItem.getUserAddressId() + " is not found"
                        );
                    }

                    return OrderItemResponse.fromOrderItemProductAndAddress(
                            orderItem,
                            product,
                            userAddress
                    );
                })
                .toList();
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order with id " + orderId + " not found"
                ));

        if(!OrderStateTransition .isValidTransition(order.getStatus(),newStatus)){
            throw new IllegalStateException("Order with current status"+order.getStatus()
                    +" cannot be updated into "+newStatus);
        }
        order.setStatus(newStatus);
        orderRepository.save(order);

        if(newStatus.equals(OrderStatus.CANCELLED)){
            cancelXenditInvoice(order);
            restoreInventoryForOrder(orderId);
        }


    }



    @Override
    public PaginatedOrderResponse convertOrderPage(Page<OrderResponse> orderResponses) {
        return PaginatedOrderResponse.builder()
                .data(orderResponses.getContent())
                .pageNo(orderResponses.getNumber())
                .pageSize(orderResponses.getSize())
                .totalElements(orderResponses.getTotalElements())
                .totalPages(orderResponses.getTotalPages())
                .last(orderResponses.isLast())
                .build();
    }


    @Override
    public Page<OrderResponse> findOrdersByUserIdAndPageable(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdByPageable(userId, pageable)
                .map(OrderResponse::fromOrder);
    }

    @Override
    public Double calculateOrderTotal(Long orderId) {
        return orderItemRepository.calculateTotalOrder(orderId);
    }

    public void cancelXenditInvoice(Order order) {
        if (order.getXenditInvoiceId() == null || order.getXenditInvoiceId().isBlank()) {
            log.warn("Skip Xendit invoice cancellation because order {} has no xendit invoice id",
                    order.getOrderId());
            return;
        }

        try {
            Invoice invoice = Invoice.expire(order.getXenditInvoiceId());
            order.setXenditPaymentStatus(invoice.getStatus());
            orderRepository.save(order);
        } catch (XenditException e) {
            log.error("Error while requesting invoice cancellation for order with xendit id: {}",
                    order.getXenditInvoiceId(), e);
        }

    }

    @Scheduled(cron = "0 */1 * * * *")
    @Transactional
    public void cancelUnpaidOrders() {
        LocalDateTime cancelThreshold = LocalDateTime.now().minusDays(1);
        List<Order> unpaidOrders = orderRepository.findByStatusAndOrderDateBefore(OrderStatus.PENDING,
                cancelThreshold);
        for (Order order : unpaidOrders) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            cancelXenditInvoice(order);
            restoreInventoryForOrder(order.getOrderId());
        }
    }

    private void restoreInventoryForOrder(Long orderId) {
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        if (orderItems.isEmpty()) {
            return;
        }

        Map<Long, Integer> productQuantities = orderItems.stream()
                .collect(Collectors.toMap(
                        OrderItem::getProductId,
                        OrderItem::getQuantity,
                        Integer::sum
                ));

        inventoryService.increaseQuantity(productQuantities);
    }
}
