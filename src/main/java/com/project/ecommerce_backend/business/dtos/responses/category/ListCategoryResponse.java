package com.project.ecommerce_backend.business.dtos.responses.category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListCategoryResponse {
    private Long id;
    
    private String name;
    
    private Long parentCategoryId;
    
    private String parentCategoryName;
}
