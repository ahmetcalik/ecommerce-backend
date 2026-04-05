package com.project.ecommerce_backend.business.dtos.responses.product;
import com.project.ecommerce_backend.business.dtos.responses.common.CategoryInfoResponse;
import com.project.ecommerce_backend.business.dtos.responses.common.SupplierInfoResponse;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddProductResponse {
    private Long id;
    private String name;
    private String description;
    private OffsetDateTime cDate;
    private SupplierInfoResponse supplier;
    private List<CategoryInfoResponse> categories;
    private List<ProductItemInfo> items;
}
