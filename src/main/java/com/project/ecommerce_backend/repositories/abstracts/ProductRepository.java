package com.project.ecommerce_backend.repositories.abstracts;

import com.project.ecommerce_backend.entities.concretes.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsByProductCategories_CategoryId(Long categoryId);

    @Query("SELECT p FROM Product p " +
            "LEFT JOIN FETCH p.supplier " +
            "LEFT JOIN FETCH p.productCategories pc LEFT JOIN FETCH pc.category " +
            "LEFT JOIN FETCH p.productImages " +
            "LEFT JOIN FETCH p.productItems pi LEFT JOIN FETCH pi.colour LEFT JOIN FETCH pi.size " +
            "WHERE p.id = :id")
    Optional<Product> findByIdWithDetails(@Param("id") Long id);

    boolean existsByName(String name);

    Optional<Product> findByNameAndIdNot(String name, Long id);

}
