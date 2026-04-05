package com.project.ecommerce_backend.business.dtos.events;

import com.project.ecommerce_backend.core.constants.Messages;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = Messages.Validations.Order.ORDER_ID_CAN_NOT_BE_NULL)
    private Long orderId;

    @NotNull(message = Messages.Validations.Order.CUSTOMER_ID_CAN_NOT_BE_NULL)
    private Long customerId;

    @NotNull(message = Messages.Validations.Order.TOTAL_AMOUNT_MUST_BE_GREATER_THAN_ZERO)
    @Positive(message = Messages.Validations.Order.TOTAL_AMOUNT_MUST_BE_GREATER_THAN_ZERO)
    private BigDecimal orderTotal;

    @NotNull(message = Messages.Validations.Order.PAYMENT_METHOD_ID_CAN_NOT_BE_NULL)
    private Long paymentMethodId;

    @NotNull(message = Messages.Validations.Order.INSTALLMENT_COUNT_MIN_ONE)
    @Min(value = 1, message = Messages.Validations.Order.INSTALLMENT_COUNT_MIN_ONE)
    private Integer installmentCount;

    @Valid
    @NotEmpty(message = Messages.Validations.Product.PRODUCT_ITEMS_SIZE_MIN)
    private List<OrderItemDetail> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDetail implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        @NotNull(message = Messages.Validations.Product.PRODUCT_ITEM_ID_CAN_NOT_BE_NULL)
        private Long productItemId;

        @Min(value = 1, message = Messages.Validations.Cart.QUANTITY_MUST_BE_AT_LEAST_ONE)
        private int quantity;
    }
}