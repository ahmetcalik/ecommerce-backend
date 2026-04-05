package com.project.ecommerce_backend.business.dtos.responses.cart;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Kullanıcının tüm alışveriş sepetini temsil eden ana veri yapısı.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartResponse {
    /**
     * Alışveriş sepetinin ID'si (shopping_cart.id)
     */
    private Long id;

    /**
     * Sepetteki tüm ürün satırlarının listesi.
     */
    private List<CartItemResponse> items;

    /**
     * Sepetteki tüm ürünlerin toplam adedi.
     */
    private Integer totalItems;

    private BigDecimal totalSubTotal; // Ürünlerin KDV'siz Toplamı

    private BigDecimal totalVatAmount; // Hesaplanan Toplam KDV Tutarı

    private BigDecimal grandTotal; // Ürünlerin KDV Dahil Toplamı (subTotal + vatAmount)
}
