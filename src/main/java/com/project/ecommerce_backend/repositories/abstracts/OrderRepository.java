package com.project.ecommerce_backend.repositories.abstracts;

import com.project.ecommerce_backend.entities.concretes.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerId(Long customerId);

    boolean existsByShippingAddressId(Long addressId);

    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.customer " +
            "LEFT JOIN FETCH o.shippingAddress sa LEFT JOIN FETCH sa.country " +
            "LEFT JOIN FETCH o.shippingMethod " +
            "LEFT JOIN FETCH o.orderStatus " +
            "LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.productItem pi " +
            "LEFT JOIN FETCH pi.product LEFT JOIN FETCH pi.colour LEFT JOIN FETCH pi.size " +
            "WHERE o.id = :orderId")
    Optional<Order> findByIdWithDetails(@Param("orderId") Long orderId);

    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.orderStatus " +
            "WHERE o.customer.id = :customerId")
    Slice<Order> findSliceByCustomerId(@Param("customerId") Long customerId, Pageable pageable);

    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.customer " +
            "LEFT JOIN FETCH o.shippingAddress sa " +
            "LEFT JOIN FETCH sa.country " +
            "LEFT JOIN FETCH o.shippingMethod " +
            "LEFT JOIN FETCH o.orderStatus " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.productItem pi " +
            "LEFT JOIN FETCH pi.product " +
            "LEFT JOIN FETCH pi.colour " +
            "LEFT JOIN FETCH pi.size " +
            "WHERE o.id = :orderId AND o.customer.id = :customerId")
    Optional<Order> findByIdAndCustomerIdWithDetails(@Param("orderId") Long orderId, @Param("customerId") Long customerId);

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN TRUE ELSE FALSE END " +
            "FROM Order o JOIN o.orderItems oi JOIN oi.productItem pi JOIN pi.product p " +
            "WHERE o.id = :orderId AND p.supplier.cUser = :supplierId")
    boolean existsByOrderIdAndOrderItemProductItemProductSupplierId(
            @Param("orderId") Long orderId,
            @Param("supplierId") Long supplierId);

}