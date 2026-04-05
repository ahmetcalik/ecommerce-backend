package com.project.ecommerce_backend.business.dtos.requests.address;

import com.project.ecommerce_backend.core.constants.Messages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AddressRequest {

    @NotBlank(message = Messages.Validations.Address.ADDRESS_TITLE_CAN_NOT_BE_BLANK)
    @Size(min = 2, max = 50, message = Messages.Validations.Address.ADDRESS_TITLE_SIZE_MIN_MAX)
    private String title;

    @NotBlank(message = Messages.Validations.Address.ADDRESS_LINE1_CAN_NOT_BE_BLANK)
    @Size(max = 255, message = Messages.Validations.Address.ADDRESS_LINE1_SIZE_MAX)
    private String addressLine1;

    @Size(max = 255, message = Messages.Validations.Address.ADDRESS_LINE2_SIZE_MAX)
    private String addressLine2;

    @NotBlank(message = Messages.Validations.Address.ADDRESS_CITY_CAN_NOT_BE_BLANK)
    @Size(max = 100, message = Messages.Validations.Address.ADDRESS_CITY_SIZE_MAX)
    private String city;

    @NotBlank(message = Messages.Validations.Address.ADDRESS_POSTAL_CODE_CAN_NOT_BE_BLANK)
    @Pattern(regexp = "^[0-9]{5}$", message = Messages.Validations.Address.ADDRESS_POSTAL_CODE_PATTERN_INVALID)
    private String postalCode;

    @NotNull(message = Messages.Validations.Address.ADDRESS_COUNTRY_ID_CAN_NOT_BE_NULL)
    private Long countryId;

    @NotNull(message = Messages.Validations.Address.ADDRESS_SHIPPING_FLAG_CAN_NOT_BE_NULL)
    private Boolean isShippingAddress;

    @NotNull(message = Messages.Validations.Address.ADDRESS_BILLING_FLAG_CAN_NOT_BE_NULL)
    private Boolean isBillingAddress;
}
