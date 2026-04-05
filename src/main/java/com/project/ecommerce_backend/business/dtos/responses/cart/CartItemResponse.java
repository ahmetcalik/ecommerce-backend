package com.project.ecommerce_backend.business.dtos.responses.cart;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Bir alışveriş sepetindeki tek bir ürün satırını temsil eden veri yapısı.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItemResponse {
    /**
     * Sepet ürün satırının ID'si (shopping_cart_item.id)
     */
    private Long id;

    /**
     * Ürün varyantının ID'si (product_item.id)
     */
    private Long productItemId;

    private String productName;
    private String colourName;
    private String sizeName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;
    private String mainImageUrl;
}
