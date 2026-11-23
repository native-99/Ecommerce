package com.izi.ecommerce.service;


import com.izi.ecommerce.entity.Order;
import com.izi.ecommerce.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OrderService {

    OrderResponse checkout(CheckoutRequest checkoutRequest);

    Optional<Order> findOrderById(Long orderId);

    Order findOrderOwnedByUser(Long orderId, Long userId);

    List<Order> findOrdersByUserId(Long userId);

    List<Order> findOrdersByStatus(OrderStatus status);

    void cancelOrder(Long orderId);

    List<OrderItemResponse> findOrderItemsByOrderId(Long orderId);

    void updateOrderStatus(Long orderId, OrderStatus newStatus);

    Double calculateOrderTotal(Long orderId);

    Page<OrderResponse> findOrdersByUserIdAndPageable(Long userId, Pageable pageable);

    PaginatedOrderResponse convertOrderPage(Page<OrderResponse> orderPage);
}