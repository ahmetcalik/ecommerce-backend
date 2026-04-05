package com.project.ecommerce_backend.business.dtos.responses.category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCategoryResponse {
    private Long id;
    
    private String name;
    
    private String description;
    
    private Long parentCategoryId;
    
    private Boolean isActive;
    
    private OffsetDateTime uDate;
}