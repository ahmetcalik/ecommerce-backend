package com.project.ecommerce_backend.business.dtos.responses.order;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemResponse {
    private Long productId;
    private String productName;
    private String colourName;
    private String sizeName;
    private int quantity;
    private BigDecimal priceAtOrder; // Sipariş anındaki KDV'siz birim fiyat
    private BigDecimal vatRate;      // Sipariş anındaki KDV oranı
}