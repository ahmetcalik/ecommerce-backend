package com.project.ecommerce_backend.business.abstracts;

import com.project.ecommerce_backend.business.dtos.responses.installment.InstallmentOptionResponse;
import java.math.BigDecimal;
import java.util.List;

public interface InstallmentService {

    List<InstallmentOptionResponse> getAvailableOptions(BigDecimal totalAmount, String cardFamily);

    InstallmentOptionResponse getSpecificOption(BigDecimal totalAmount, String cardFamily, int installmentCount);

}