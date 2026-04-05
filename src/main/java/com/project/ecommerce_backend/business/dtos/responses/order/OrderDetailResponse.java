package com.project.ecommerce_backend.business.dtos.responses.order;

import com.project.ecommerce_backend.business.dtos.responses.address.AddressDetailResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class OrderDetailResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private AddressDetailResponse shippingAddress;
    private String shippingMethodName;
    private String orderStatusName;
    private OffsetDateTime orderDate;
    private BigDecimal orderTotal;
    private List<OrderItemResponse> items; // Siparişin içindeki ürünler için yeni bir DTO
}