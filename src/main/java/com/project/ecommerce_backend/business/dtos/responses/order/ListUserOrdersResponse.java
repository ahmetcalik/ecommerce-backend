package com.project.ecommerce_backend.business.dtos.responses.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListUserOrdersResponse {
    private Long id; // Sipariş ID
    private OffsetDateTime orderDate; // Sipariş Tarihi
    private BigDecimal orderTotal; // Toplam Tutar
    private String orderStatusName; // Siparişin Son Durumu (örn: "Teslim Edildi")
}