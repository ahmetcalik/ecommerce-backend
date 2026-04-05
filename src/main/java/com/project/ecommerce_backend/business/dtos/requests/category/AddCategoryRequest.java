package com.project.ecommerce_backend.business.dtos.requests.category;

import com.project.ecommerce_backend.core.constants.Messages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddCategoryRequest {

    @NotBlank(message = Messages.Validations.Category.CATEGORY_NAME_CAN_NOT_BE_BLANK)
    @Size(min = 2, max = 50, message = Messages.Validations.Category.CATEGORY_NAME_SIZE_MIN_MAX)
    private String name;

    private Long parentCategoryId;

    @Size(max = 500, message = Messages.Validations.Category.CATEGORY_DESCRIPTION_SIZE_MAX)
    private String description;

    @NotNull(message = Messages.Validations.Category.CATEGORY_IS_ACTIVE_NOT_NULL)
    private Boolean isActive;
}