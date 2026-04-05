package com.project.ecommerce_backend.business.dtos.requests.return_request;

import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.entities.enums.ReturnStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateReturnStatusRequest {

    @NotNull(message = Messages.Validations.Return.NEW_STATUS_CAN_NOT_BE_NULL)
    private ReturnStatusEnum newStatus;
}