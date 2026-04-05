package com.project.ecommerce_backend.entities.concretes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Alışveriş sepetine eklenen her bir ürün kalemini ve bu kalemin miktar bilgisini yöneten varlık sınıfıdır.
 * Belirli bir sepeti ürün varyantlarıyla (ProductItem) ilişkilendirerek; sepet içeriğinin güncellenmesi, miktar kontrolü ve sipariş öncesi ürün toplama süreçlerinin teknik takibini sağlar.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shopping_cart_item")
public class ShoppingCartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    @JsonIgnore
    private ShoppingCart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_item_id", nullable = false)
    private ProductItem productItem;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;
}
