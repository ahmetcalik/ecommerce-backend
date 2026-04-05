package com.project.ecommerce_backend.business.dtos.requests.auth;

import com.project.ecommerce_backend.core.constants.Messages;
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
public class RefreshTokenRequest {

    @NotBlank(message = Messages.Validations.Auth.REFRESH_TOKEN_CAN_NOT_BE_BLANK)
    @Size(min = 20, max = 2000)
    private String refreshToken;
}
