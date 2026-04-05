package com.project.ecommerce_backend.repositories.abstracts;

import com.project.ecommerce_backend.entities.concretes.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long> {

    Optional<ShoppingCart> findByCustomerId(Long customerId);

    Optional<ShoppingCart> findFirstBySessionId(UUID sessionId);

    @Query("SELECT sc FROM ShoppingCart sc " +
            "LEFT JOIN FETCH sc.cartItems ci " +
            "LEFT JOIN FETCH ci.productItem pi " +
            "LEFT JOIN FETCH pi.product p " +
            "LEFT JOIN FETCH p.productImages " +
            "LEFT JOIN FETCH pi.colour " +
            "LEFT JOIN FETCH pi.size " +
            "WHERE sc.id = :cartId")
    Optional<ShoppingCart> findByIdWithDetails(@Param("cartId") Long cartId);
}

