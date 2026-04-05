package com.project.ecommerce_backend.business.dtos.requests.auth;

import com.project.ecommerce_backend.core.constants.Messages;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = Messages.Validations.Auth.EMAIL_CAN_NOT_BE_BLANK)
    @Email(message = Messages.Validations.Auth.EMAIL_FORMAT_INVALID)
    private String email;

    @NotBlank(message = Messages.Validations.Auth.PASSWORD_CAN_NOT_BE_BLANK)
    @Size(min = 8, max = 64, message = Messages.Validations.Auth.PASSWORD_SIZE_MIN_MAX)
    private String password;
}