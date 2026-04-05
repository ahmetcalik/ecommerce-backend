package com.project.ecommerce_backend.business.dtos.responses.payment;

import lombok.Data;

@Data
public class PaymentMethodResponse {
    private Long id;
    private String cardHolderName;
    private String maskedCardNumber; // Örn: "**** **** **** 1234"
    private String cardFamily; // Örn: "Bonus", "World"
    private Integer expiryMonth;
    private Integer expiryYear;
}