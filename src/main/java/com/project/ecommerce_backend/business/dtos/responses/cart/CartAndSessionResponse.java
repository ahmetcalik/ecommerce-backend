package com.project.ecommerce_backend.business.dtos.responses.cart;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartAndSessionResponse {
    private CartResponse cartResponse;
    private UUID sessionId;
}