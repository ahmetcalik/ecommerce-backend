package com.project.ecommerce_backend.business.dtos.requests.product;

import com.project.ecommerce_backend.core.constants.Messages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddProductImageRequest {

    @NotBlank(message = Messages.Validations.Product.IMAGE_URL_CAN_NOT_BE_BLANK)
    @Pattern(regexp = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", message = Messages.Validations.Product.IMAGE_URL_INVALID_FORMAT)
    private String imageUrl;

    private boolean isMainImage = false;
}