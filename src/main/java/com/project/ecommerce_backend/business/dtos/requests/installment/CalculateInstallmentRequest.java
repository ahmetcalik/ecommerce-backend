package com.project.ecommerce_backend.business.dtos.requests.installment;

import com.project.ecommerce_backend.core.constants.Messages;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CalculateInstallmentRequest {

    @NotBlank(message = Messages.Validations.Installment.BIN_NUMBER_CAN_NOT_BE_BLANK)
    @Size(min = 6, max = 8, message = Messages.Validations.Installment.BIN_NUMBER_SIZE_INVALID)
    @Pattern(regexp = "^[0-9]*$", message = Messages.Validations.Installment.BIN_NUMBER_PATTERN_INVALID)
    private String binNumber;

    @NotNull(message = Messages.Validations.Installment.AMOUNT_CAN_NOT_BE_NULL)
    @DecimalMin(value = "0.01", message = Messages.Validations.Installment.AMOUNT_MUST_BE_GREATER_THAN_ZERO)
    private BigDecimal amount;
}