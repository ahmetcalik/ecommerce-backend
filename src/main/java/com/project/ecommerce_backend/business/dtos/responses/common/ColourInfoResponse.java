package com.project.ecommerce_backend.business.dtos.responses.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ColourInfoResponse {
    private Long id;
    private String colourName;
}
