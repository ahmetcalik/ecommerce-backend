package com.project.ecommerce_backend.business.dtos.requests.payment;

import com.project.ecommerce_backend.core.constants.Messages;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AddPaymentMethodRequest {

    @NotBlank(message = Messages.Validations.Payment.CARD_HOLDER_NAME_CAN_NOT_BE_BLANK)
    @Size(min = 3, max = 50)
    private String cardHolderName;

    @NotBlank(message = Messages.Validations.Payment.CARD_NUMBER_PATTERN_INVALID)
    @Pattern(regexp = "^[0-9]{16}$", message = Messages.Validations.Payment.CARD_NUMBER_PATTERN_INVALID)
    private String cardNumber;

    @NotNull(message = Messages.Validations.Payment.EXPIRY_MONTH_INVALID)
    @Min(value = 1, message = Messages.Validations.Payment.EXPIRY_MONTH_INVALID)
    @Max(value = 12, message = Messages.Validations.Payment.EXPIRY_MONTH_INVALID)
    private Integer expiryMonth;

    @NotNull(message = Messages.Validations.Payment.EXPIRY_YEAR_INVALID)
    private Integer expiryYear;

    @NotBlank(message = Messages.Validations.Payment.CVC_PATTERN_INVALID)
    @Pattern(regexp = "^[0-9]{3,4}$", message = Messages.Validations.Payment.CVC_PATTERN_INVALID)
    private String cvc;

    @AssertTrue(message = Messages.Validations.Payment.EXPIRY_YEAR_INVALID)
    private boolean isExpiryYearValid() {
        if (expiryYear == null) return false;
        int currentYear = java.time.Year.now().getValue();
        return expiryYear >= currentYear;
    }
}