package com.project.ecommerce_backend.business.dtos.responses.address;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressDetailResponse {
    private Long id;
    private String title;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String postalCode;
    private String countryName;
    private boolean isDefaultShipping;
    private boolean isDefaultBilling;
}