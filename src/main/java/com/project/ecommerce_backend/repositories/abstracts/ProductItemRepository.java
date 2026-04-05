package com.project.ecommerce_backend.repositories.abstracts;

import com.project.ecommerce_backend.entities.concretes.ProductItem;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductItemRepository extends JpaRepository<ProductItem, Long> {

    @Modifying
    @Query("DELETE FROM ProductItem pi WHERE pi.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);

}
