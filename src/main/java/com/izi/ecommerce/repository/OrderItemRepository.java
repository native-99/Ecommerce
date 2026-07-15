package com.izi.ecommerce.repository;

import com.izi.ecommerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query(value = """
            SELECT oi.* FROM order_items oi
            JOIN orders o ON o.order_id = oi.order_id
            WHERE o.user_id = :userId
            AND oi.product_id = :productId
            """, nativeQuery = true)
    List<OrderItem> findByUserAndProduct(
            @Param("userId") Long userId,
            @Param("productId") Long productId
    );

    @Query(value = """
            SELECT COALESCE(SUM(quantity * price), 0)
            FROM order_items
            WHERE order_id = :orderId
            """, nativeQuery = true)
    Double calculateTotalOrder(@Param("orderId") Long orderId);
}
