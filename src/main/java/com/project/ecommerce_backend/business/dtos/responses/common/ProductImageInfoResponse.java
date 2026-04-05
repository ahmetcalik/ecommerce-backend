package com.project.ecommerce_backend.business.dtos.responses.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductImageInfoResponse {
    private Long id;
    private String imageUrl;
    private boolean isMainImage;
}