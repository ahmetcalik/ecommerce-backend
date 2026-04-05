package com.project.ecommerce_backend.business.dtos.responses.order;

import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class AddOrderResponse {
    private Long id;
    private Long customerId;
    private BigDecimal orderTotal;
    private OffsetDateTime orderDate;
    private String orderStatusName;
}