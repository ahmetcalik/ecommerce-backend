package com.project.ecommerce_backend.business.dtos.requests.product;

import com.project.ecommerce_backend.core.constants.Messages;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddProductItemRequest {

    @NotBlank(message = Messages.Validations.Product.SKU_CAN_NOT_BE_BLANK)
    @Size(max = 255, message = Messages.Validations.Product.SKU_SIZE_MAX)
    private String sku;

    @NotNull(message = Messages.Validations.Product.STOCK_CAN_NOT_BE_NULL)
    @PositiveOrZero(message = Messages.Validations.Product.STOCK_MUST_BE_POSITIVE_OR_ZERO)
    private Integer quantityInStock;

    @NotNull(message = Messages.Validations.Product.PRICE_CAN_NOT_BE_NULL)
    @Positive(message = Messages.Validations.Product.PRICE_MUST_BE_POSITIVE)
    @DecimalMin(value = "0.01", message = Messages.Validations.Product.PRICE_MUST_BE_POSITIVE)
    private BigDecimal unitPrice;

    @NotNull(message = Messages.Validations.Product.COLOUR_ID_CAN_NOT_BE_NULL)
    private Long colourId;

    @NotNull(message = Messages.Validations.Product.SIZE_ID_CAN_NOT_BE_NULL)
    private Long sizeId;
}