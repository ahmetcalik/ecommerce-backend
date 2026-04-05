package com.project.ecommerce_backend.business.dtos.responses.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListProductsResponse {

    private Long id;
    private String name;
    private String mainImageUrl;
    private BigDecimal price;
    private String categoryName;
}