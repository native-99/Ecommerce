package com.izi.ecommerce.service;

import com.izi.ecommerce.common.errors.ForbiddenAccessException;
import com.izi.ecommerce.common.errors.ResourceNotFoundException;
import com.izi.ecommerce.entity.CartItem;
import com.izi.ecommerce.entity.Order;
import com.izi.ecommerce.entity.Product;
import com.izi.ecommerce.entity.User;
import com.izi.ecommerce.entity.UserAddress;
import com.izi.ecommerce.model.CheckoutRequest;
import com.izi.ecommerce.model.OrderResponse;
import com.izi.ecommerce.model.OrderStatus;
import com.izi.ecommerce.model.PaymentResponse;
import com.izi.ecommerce.model.ShippingRateResponse;
import com.izi.ecommerce.repository.CartItemRepository;
import com.izi.ecommerce.repository.OrderItemRepository;
import com.izi.ecommerce.repository.OrderRepository;
import com.izi.ecommerce.repository.ProductRepository;
import com.izi.ecommerce.repository.UserAddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.izi.ecommerce.common.errors.InventoryException;
import static org.mockito.Mockito.never;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserAddressRepository userAddressRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ShippingService shippingService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private CheckoutRequest checkoutRequest;
    private List<CartItem> cartItems;
    private UserAddress userAddress;
    private Product product;
    private Product secondProduct;
    private UserAddress sellerAddress;
    private User seller;
    private User buyer;

    @BeforeEach
    void setUp() {
        seller = new User();
        seller.setUserId(1L);

        buyer = new User();
        buyer.setUserId(2L);

        checkoutRequest = new CheckoutRequest();
        checkoutRequest.setUserId(buyer.getUserId());
        checkoutRequest.setUserAddressId(1L);
        checkoutRequest.setSelectedCartItemIds(Arrays.asList(1L, 2L));

        cartItems = new ArrayList<>();

        CartItem cartItem1 = new CartItem();
        cartItem1.setCartItemId(1L);
        cartItem1.setProductId(1L);
        cartItem1.setQuantity(2);
        cartItem1.setPrice(new BigDecimal("100.00"));
        cartItems.add(cartItem1);

        CartItem cartItem2 = new CartItem();
        cartItem2.setCartItemId(2L);
        cartItem2.setProductId(2L);
        cartItem2.setQuantity(1);
        cartItem2.setPrice(new BigDecimal("50.00"));
        cartItems.add(cartItem2);

        userAddress = new UserAddress();
        userAddress.setUserAddressId(1L);
        userAddress.setUserId(buyer.getUserId());
        userAddress.setStreetAddress("Buyer Street");
        userAddress.setCity("Jakarta");
        userAddress.setState("DKI Jakarta");
        userAddress.setPostalCode("12345");

        product = new Product();
        product.setProductId(1L);
        product.setWeight(new BigDecimal("0.5"));
        product.setUserId(seller.getUserId());

        secondProduct = new Product();
        secondProduct.setProductId(2L);
        secondProduct.setWeight(new BigDecimal("1.0"));
        secondProduct.setUserId(seller.getUserId());

        sellerAddress = new UserAddress();
        sellerAddress.setUserAddressId(2L);
        sellerAddress.setUserId(seller.getUserId());
        sellerAddress.setStreetAddress("Seller Street");
        sellerAddress.setCity("Bandung");
        sellerAddress.setState("Jawa Barat");
        sellerAddress.setPostalCode("54321");
    }

    @Test
    void testCheckout_WhenCartIsEmpty() {
        when(cartItemRepository.getUserCartItems(buyer.getUserId()))
                .thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.checkout(checkoutRequest);
        });
    }

    @Test
    void testCheckout_WhenAddressIsNotFound() {
        when(cartItemRepository.getUserCartItems(buyer.getUserId()))
                .thenReturn(cartItems);
        when(userAddressRepository.findById(checkoutRequest.getUserAddressId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.checkout(checkoutRequest);
        });

        verify(inventoryService, never()).checkAndLockInventory(anyMap());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCheckout_WhenCartItemBelongsToAnotherUser() {
        // Buyer's cart only holds item 1; item 2 belongs to someone else's cart.
        when(cartItemRepository.getUserCartItems(buyer.getUserId()))
                .thenReturn(List.of(cartItems.get(0)));

        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.checkout(checkoutRequest);
        });

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCheckout_WhenAddressBelongsToAnotherUser() {
        when(cartItemRepository.getUserCartItems(buyer.getUserId()))
                .thenReturn(cartItems);
        when(userAddressRepository.findById(checkoutRequest.getUserAddressId()))
                .thenReturn(Optional.of(sellerAddress));

        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.checkout(checkoutRequest);
        });

        verify(inventoryService, never()).checkAndLockInventory(anyMap());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCheckout_WhenInventoryCannotBeLocked() {
        when(cartItemRepository.getUserCartItems(buyer.getUserId()))
                .thenReturn(cartItems);
        when(userAddressRepository.findById(checkoutRequest.getUserAddressId()))
                .thenReturn(Optional.of(userAddress));
        when(inventoryService.checkAndLockInventory(anyMap()))
                .thenReturn(false);

        assertThrows(InventoryException.class, () -> {
            orderService.checkout(checkoutRequest);
        });

        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentService, never()).create(any(Order.class));
    }

    @Test
    void testCheckout_WhenProductIsNotFoundForShipping() {
        when(cartItemRepository.getUserCartItems(buyer.getUserId()))
                .thenReturn(cartItems);
        when(userAddressRepository.findById(checkoutRequest.getUserAddressId()))
                .thenReturn(Optional.of(userAddress));
        when(inventoryService.checkAndLockInventory(anyMap()))
                .thenReturn(true);
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    if (order.getOrderId() == null) {
                        order.setOrderId(1L);
                    }
                    return order;
                });
        when(productRepository.findById(1L))
                .thenReturn(Optional.empty());
        when(productRepository.findById(2L))
                .thenReturn(Optional.empty());
        when(paymentService.create(any(Order.class)))
                .thenReturn(PaymentResponse.builder()
                        .xenditInvoiceId("payment123")
                        .xenditInvoiceStatus("PENDING")
                        .xenditPaymentUrl("http://payment.url")
                        .build());

        OrderResponse result = orderService.checkout(checkoutRequest);

        assertEquals(new BigDecimal("0"), result.getShippingFee());
        assertEquals(new BigDecimal("257.5000"), result.getTotalAmount());
        verify(shippingService, never()).calculateShippingRate(any());
        verify(userAddressRepository, never()).findByUserIdAndIsDefaultTrue(anyLong());
    }





    @Test
    void testCheckout_SuccessfulCheckout() {
        when(cartItemRepository.getUserCartItems(buyer.getUserId()))
                .thenReturn(cartItems);
        when(userAddressRepository.findById(checkoutRequest.getUserAddressId()))
                .thenReturn(Optional.of(userAddress));
        when(inventoryService.checkAndLockInventory(anyMap()))
                .thenReturn(true);
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    if (order.getOrderId() == null) {
                        order.setOrderId(1L);
                    }
                    return order;
                });
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));
        when(productRepository.findById(2L))
                .thenReturn(Optional.of(secondProduct));
        when(userAddressRepository.findByUserIdAndIsDefaultTrue(seller.getUserId()))
                .thenReturn(Optional.of(sellerAddress));
        when(shippingService.calculateShippingRate(any()))
                .thenReturn(ShippingRateResponse.builder()
                        .shippingFee(new BigDecimal("10.00"))
                        .estimatedDeliveryTime("2 days")
                        .build());
        when(paymentService.create(any(Order.class)))
                .thenReturn(PaymentResponse.builder()
                        .xenditInvoiceId("payment123")
                        .xenditInvoiceStatus("PENDING")
                        .xenditPaymentUrl("http://payment.url")
                        .build());

        OrderResponse result = orderService.checkout(checkoutRequest);

        assertNotNull(result);
        assertEquals(1L, result.getOrderId());
        assertEquals(buyer.getUserId(), result.getUserId());
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertEquals(new BigDecimal("250.00"), result.getSubtotal());
        assertEquals(new BigDecimal("7.5000"), result.getTaxFee());
        assertEquals(new BigDecimal("20.00"), result.getShippingFee());
        assertEquals(new BigDecimal("277.5000"), result.getTotalAmount());
        assertEquals("payment123", result.getXenditInvoiceId());
        assertEquals("PENDING", result.getXenditPaymentStatus());
        assertEquals("http://payment.url", result.getPaymentUrl());

        verify(cartItemRepository).getUserCartItems(buyer.getUserId());
        verify(userAddressRepository).findById(checkoutRequest.getUserAddressId());
        verify(inventoryService).checkAndLockInventory(anyMap());
        verify(orderRepository, times(3)).save(any(Order.class));
        verify(orderItemRepository).saveAll(any());
        verify(cartItemRepository).deleteAll(cartItems);
        verify(productRepository, times(2)).findById(anyLong());
        verify(userAddressRepository, times(2)).findByUserIdAndIsDefaultTrue(seller.getUserId());
        verify(shippingService, times(2)).calculateShippingRate(any());
        verify(paymentService).create(any(Order.class));
        verify(inventoryService).decreaseQuantity(anyMap());
    }

    @Test
    void testFindOrderOwnedByUser_ReturnsOrderForOwner() {
        Order order = Order.builder().orderId(5L).userId(buyer.getUserId()).build();
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        assertEquals(order, orderService.findOrderOwnedByUser(5L, buyer.getUserId()));
    }

    @Test
    void testFindOrderOwnedByUser_RejectsOtherUser() {
        Order order = Order.builder().orderId(5L).userId(buyer.getUserId()).build();
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        assertThrows(ForbiddenAccessException.class,
                () -> orderService.findOrderOwnedByUser(5L, seller.getUserId()));
    }

    @Test
    void testFindOrderOwnedByUser_WhenOrderIsNotFound() {
        when(orderRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.findOrderOwnedByUser(5L, buyer.getUserId()));
    }

    @Test
    void testFindAllOrdersByPageable_ReturnsOrdersOfAllUsers() {
        PageRequest pageable = PageRequest.of(0, 10);
        Order buyerOrder = Order.builder().orderId(1L).userId(buyer.getUserId()).build();
        Order sellerOrder = Order.builder().orderId(2L).userId(seller.getUserId()).build();
        when(orderRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(buyerOrder, sellerOrder)));

        Page<OrderResponse> result = orderService.findAllOrdersByPageable(pageable);

        assertEquals(2, result.getTotalElements());
    }
}
