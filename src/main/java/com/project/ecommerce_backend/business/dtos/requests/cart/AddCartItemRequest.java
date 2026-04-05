package com.project.ecommerce_backend.business.dtos.requests.cart;

import com.project.ecommerce_backend.core.constants.Messages;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddCartItemRequest {

    @NotNull(message = Messages.Validations.Cart.PRODUCT_ITEM_ID_CAN_NOT_BE_NULL)
    private Long productItemId;

    @NotNull(message = Messages.Validations.Cart.QUANTITY_CAN_NOT_BE_NULL)
    @Min(value = 1, message = Messages.Validations.Cart.QUANTITY_MUST_BE_AT_LEAST_ONE)
    @Max(value = 100, message = Messages.Validations.Cart.QUANTITY_EXCEEDS_MAX_LIMIT)
    private Integer quantity;
}
