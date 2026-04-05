package com.project.ecommerce_backend.business.dtos.responses.payment;

import lombok.Data;

@Data
public class ListPaymentMethodResponse {
    private Long id;
    private String maskedCardNumber; // Örn: "**** **** **** 1234"
    private String cardFamily;       // Örn: "Bonus", "World"
    private String cardHolderName;
}