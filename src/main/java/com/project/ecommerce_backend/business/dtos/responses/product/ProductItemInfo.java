package com.project.ecommerce_backend.business.dtos.responses.product;
import com.project.ecommerce_backend.business.dtos.responses.common.ColourInfoResponse;
import com.project.ecommerce_backend.business.dtos.responses.common.SizeInfoResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductItemInfo {
    private Long id;
    private String sku;
    private Integer quantityInStock;
    private BigDecimal unitPrice;
    private ColourInfoResponse colour;
    private SizeInfoResponse size;
}
