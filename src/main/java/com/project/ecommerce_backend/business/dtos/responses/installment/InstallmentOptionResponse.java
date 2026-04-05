package com.project.ecommerce_backend.business.dtos.responses.installment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InstallmentOptionResponse {
    private int installmentCount;
    private BigDecimal monthlyPayment;
    private BigDecimal totalAmount;
    private BigDecimal interestRate;
}