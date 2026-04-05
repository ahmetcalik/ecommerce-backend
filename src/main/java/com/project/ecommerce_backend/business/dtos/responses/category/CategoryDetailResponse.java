package com.project.ecommerce_backend.business.dtos.responses.category;

import com.project.ecommerce_backend.business.dtos.responses.common.CategoryInfoResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Detailed category information including breadcrumb and subcategories")
public class CategoryDetailResponse {
    @Schema(description = "Unique identifier of the category", example = "1")
    private Long id;
    
    @Schema(description = "Name of the category", example = "Smartphones")
    private String name;
    
    @Schema(description = "Detailed description of the category", example = "Latest smartphones and mobile devices", nullable = true)
    private String description;
    
    @Schema(description = "Whether the category is active", example = "true")
    private Boolean isActive;
    
    @ArraySchema(schema = @Schema(implementation = CategoryInfoResponse.class, description = "Breadcrumb trail showing the category hierarchy"))
    private List<CategoryInfoResponse> breadCrumb;
    
    @ArraySchema(schema = @Schema(implementation = CategoryInfoResponse.class, description = "List of direct subcategories"))
    private List<CategoryInfoResponse> subCategories;
}