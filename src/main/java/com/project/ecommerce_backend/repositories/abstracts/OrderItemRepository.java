package com.project.ecommerce_backend.repositories.abstracts;

import com.project.ecommerce_backend.entities.concretes.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT oi FROM OrderItem oi " +
            "WHERE oi.id = :orderItemId AND oi.order.customer.id = :customerId")
    Optional<OrderItem> findByIdAndOrderCustomerId(@Param("orderItemId") Long orderItemId, @Param("customerId") Long customerId);

    @Query("SELECT oi FROM OrderItem oi " +
            "WHERE oi.id = :id AND oi.order.id = :orderId AND oi.order.customer.id = :customerId")
    Optional<OrderItem> findByIdAndOrderIdAndOrderCustomerId(@Param("id") Long id, @Param("orderId") Long orderId, @Param("customerId") Long customerId);
}