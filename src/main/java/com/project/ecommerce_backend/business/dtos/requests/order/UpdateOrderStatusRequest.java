package com.project.ecommerce_backend.business.dtos.requests.order;

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
public class UpdateOrderStatusRequest {

    @NotBlank(message = Messages.Validations.Order.TRACKING_NUMBER_CAN_NOT_BE_BLANK)
    @Size(max = 100, message = Messages.Validations.Order.TRACKING_NUMBER_SIZE_MAX)
    private String trackingNumber;

    @NotNull(message = Messages.Validations.Order.CARRIER_ID_CAN_NOT_BE_NULL)
    private Long carrierId;
}