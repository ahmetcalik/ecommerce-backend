package com.project.ecommerce_backend.business.dtos.responses.payment;

import lombok.Data;

@Data
public class PaymentMethodDetailResponse {
    private Long id;
    private String cardHolderName;
    private String maskedCardNumber;
    private String cardFamily;
    private Integer expiryMonth;
    private Integer expiryYear;
}