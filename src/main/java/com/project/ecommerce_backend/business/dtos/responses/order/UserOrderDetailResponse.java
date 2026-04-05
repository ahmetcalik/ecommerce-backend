package com.project.ecommerce_backend.business.dtos.responses.order;

import com.project.ecommerce_backend.business.dtos.responses.address.AddressDetailResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class UserOrderDetailResponse {
    private Long id;
    private AddressDetailResponse shippingAddress;
    private String shippingMethodName;
    private String orderStatusName;
    private OffsetDateTime orderDate;
    private BigDecimal orderTotal;
    private List<OrderItemResponse> items;
}