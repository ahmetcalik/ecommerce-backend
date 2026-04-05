package com.project.ecommerce_backend.business.dtos.requests.product;

import com.project.ecommerce_backend.core.constants.Messages;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddProductRequest {

    @NotBlank(message = Messages.Validations.Product.PRODUCT_NAME_CAN_NOT_BE_BLANK)
    @Size(min = 2, max = 200, message = Messages.Validations.Product.PRODUCT_NAME_SIZE_MIN)
    private String name;

    @Size(max = 2000, message = Messages.Validations.Product.PRODUCT_DESCRIPTION_SIZE_MAX)
    private String description;

    @NotNull(message = Messages.Validations.Product.CATEGORY_IDS_CAN_NOT_BE_NULL)
    @Size(min = 1, message = Messages.Validations.Product.CATEGORY_IDS_SIZE_MIN)
    private List<Long> categoryIds;

    @Valid
    @NotNull(message = Messages.Validations.Product.PRODUCT_ITEMS_CAN_NOT_BE_NULL)
    @Size(min = 1, message = Messages.Validations.Product.PRODUCT_ITEMS_SIZE_MIN)
    private List<AddProductItemRequest> items;

    @Valid
    private List<AddProductImageRequest> images;
}