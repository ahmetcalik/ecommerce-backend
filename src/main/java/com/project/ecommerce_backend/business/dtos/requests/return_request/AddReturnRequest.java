package com.project.ecommerce_backend.business.dtos.requests.return_request;

import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.entities.enums.ReturnReasonEnum;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AddReturnRequest {

    @NotNull(message = Messages.Validations.Return.ORDER_ID_CAN_NOT_BE_NULL)
    private Long orderId;

    @NotNull(message = Messages.Validations.Return.ORDER_ITEM_ID_CAN_NOT_BE_NULL)
    private Long orderItemId;

    @NotNull(message = Messages.Validations.Return.REASON_ENUM_CAN_NOT_BE_NULL)
    private ReturnReasonEnum reason;

    @NotBlank(message = Messages.Validations.Return.CUSTOM_REASON_CAN_NOT_BE_BLANK)
    @Size(max = 1000, message = Messages.Validations.Return.CUSTOM_REASON_SIZE_MAX)
    private String customReason;
}