package com.project.ecommerce_backend.business.dtos.responses.category;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryTreeResponse {
    private Long id;
    private String name;
    private Long parentCategoryId;
    private List<CategoryTreeResponse> children = new ArrayList<>(); // Ağaç yapısı oluşturmak için
}