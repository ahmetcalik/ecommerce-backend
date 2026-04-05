package com.project.ecommerce_backend.repositories.abstracts;

import com.project.ecommerce_backend.business.dtos.responses.category.ListCategoryResponse;
import com.project.ecommerce_backend.entities.concretes.Category;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository <Category, Long>{

    @Query(value = "SELECT new com.project.ecommerce_backend.business.dtos.responses.category.ListCategoryResponse(" +
            "c.id, c.name, pc.id, pc.name) FROM Category c " +
            "LEFT JOIN c.parentCategory pc")
    List<ListCategoryResponse> getAll();

    @Query(value = "SELECT new com.project.ecommerce_backend.business.dtos.responses.category.ListCategoryResponse(" +
            "c.id, c.name, pc.id, pc.name) FROM Category c " +
            "LEFT JOIN c.parentCategory pc",
            countQuery = "SELECT count(c) FROM Category c")
    Slice<ListCategoryResponse> getAllWithPagination(Pageable pageable);

    @Query("SELECT c FROM Category c " +
            "LEFT JOIN FETCH c.parentCategory " +
            "LEFT JOIN FETCH c.categories " +
            "WHERE c.id = :id")
    Optional<Category> findByIdWithDetails(@Param("id") Long id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByParentCategoryId(Long parentId);

}
