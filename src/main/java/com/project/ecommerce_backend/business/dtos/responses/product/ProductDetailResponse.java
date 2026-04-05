package com.project.ecommerce_backend.business.dtos.responses.product;

import com.project.ecommerce_backend.business.dtos.responses.common.CategoryInfoResponse;
import com.project.ecommerce_backend.business.dtos.responses.common.ProductImageInfoResponse;
import com.project.ecommerce_backend.business.dtos.responses.common.SupplierInfoResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDetailResponse {
    private Long id;
    private String name;
    private String description;
    private OffsetDateTime cDate;
    private OffsetDateTime uDate;
    private SupplierInfoResponse supplier;
    private List<CategoryInfoResponse> categories;
    private List<ProductImageInfoResponse> images;
    private List<ProductItemInfo> items;
}