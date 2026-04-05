package com.project.ecommerce_backend.business.dtos.requests.order;

import com.project.ecommerce_backend.core.constants.Messages;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddOrderRequest {

    @NotNull(message = Messages.Validations.Order.CUSTOMER_ID_CAN_NOT_BE_NULL)
    private Long customerId;

    @NotNull(message = Messages.Validations.Order.SHIPPING_ADDRESS_ID_CAN_NOT_BE_NULL)
    private Long shippingAddressId;

    @NotNull(message = Messages.Validations.Order.SHIPPING_METHOD_ID_CAN_NOT_BE_NULL)
    private Long shippingMethodId;

    @NotNull(message = Messages.Validations.Order.PAYMENT_METHOD_ID_CAN_NOT_BE_NULL)
    private Long paymentMethodId;

    @NotNull(message = Messages.Validations.Order.INSTALLMENT_COUNT_MIN_ONE)
    @Min(value = 1, message = Messages.Validations.Order.INSTALLMENT_COUNT_MIN_ONE)
    private Integer installmentCount;

    @NotNull(message = Messages.Validations.Order.TOTAL_AMOUNT_MUST_BE_GREATER_THAN_ZERO)
    @DecimalMin(value = "0.01", message = Messages.Validations.Order.TOTAL_AMOUNT_MUST_BE_GREATER_THAN_ZERO)
    private BigDecimal expectedTotalAmount;
}