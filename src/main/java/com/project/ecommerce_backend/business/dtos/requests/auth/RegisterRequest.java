package com.project.ecommerce_backend.business.dtos.requests.auth;

import com.project.ecommerce_backend.core.constants.Messages;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = Messages.Validations.Auth.CONTACT_NAME_CAN_NOT_BE_BLANK)
    @Size(min = 2, max = 50, message = Messages.Validations.Auth.CONTACT_NAME_SIZE_MIN_MAX)
    private String contactName;

    @Email(message = Messages.Validations.Auth.EMAIL_FORMAT_INVALID)
    @NotBlank(message = Messages.Validations.Auth.EMAIL_CAN_NOT_BE_BLANK)
    private String emailAddress;

    @NotBlank(message = Messages.Validations.Auth.PASSWORD_CAN_NOT_BE_BLANK)
    @Size(min = 8, max = 64, message = Messages.Validations.Auth.PASSWORD_SIZE_MIN_MAX)
    private String password;

    @NotBlank(message = Messages.Validations.Auth.PHONE_NUMBER_CAN_NOT_BE_BLANK)
    @Pattern(regexp = "^(\\+90|0)?\\d{10}$", message = Messages.Validations.Auth.PHONE_NUMBER_PATTERN_INVALID)
    private String phoneNumber;
}