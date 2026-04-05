package com.project.ecommerce_backend.business.dtos.requests.category;

import com.project.ecommerce_backend.core.constants.Messages;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCategoryRequest {

    @NotNull(message = Messages.Validations.Category.CATEGORY_ID_CAN_NOT_BE_NULL)
    private Long id;

    @NotBlank(message = Messages.Validations.Category.CATEGORY_NAME_CAN_NOT_BE_BLANK)
    @Size(min = 2, max = 50, message = Messages.Validations.Category.CATEGORY_NAME_SIZE_MIN_MAX)
    private String name;

    @Size(max = 500, message = Messages.Validations.Category.CATEGORY_DESCRIPTION_SIZE_MAX)
    private String description;

    private Long parentCategoryId;

    @NotNull(message = Messages.Validations.Category.CATEGORY_IS_ACTIVE_NOT_NULL)
    private Boolean isActive;

    @AssertTrue(message = Messages.Validations.Category.CATEGORY_PARENT_CAN_NOT_BE_ITSELF)
    private boolean isValidParent() {
        return parentCategoryId == null || !parentCategoryId.equals(id);
    }
}