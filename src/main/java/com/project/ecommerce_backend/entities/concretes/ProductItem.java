package com.project.ecommerce_backend.entities.concretes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Ürünlerin renk, beden ve stok bilgilerini içeren en alt seviyedeki varyant yönetim sınıfıdır.
 * Her bir varyant için benzersiz bir stok kodu (SKU) tanımlayarak; ürünün farklı fiziksel kombinasyonlarının stok miktarını, birim fiyatını ve satış geçmişini bağımsız birer kalem olarak takip etmeye olanak tanır.
 */
@Getter
@Setter
@Entity
@Table(name = "product_item", indexes = {
        @Index(name = "idx_product_item_product_id", columnList = "product_id"),
        @Index(name = "idx_product_item_colour_id", columnList = "colour_id"),
        @Index(name = "idx_product_item_size_id", columnList = "size_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "product_item_sku_key", columnNames = {"sku"})
})
public class ProductItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product product;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "colour_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Colour colour;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "size_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private com.project.ecommerce_backend.entities.concretes.Size size;

    @Size(max = 255)
    @NotNull
    @Column(name = "sku", nullable = false)
    private String sku;

    @NotNull
    @Column(name = "quantity_in_stock", nullable = false)
    private Integer quantityInStock;

    @NotNull
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @OneToMany(mappedBy = "productItem")
    @JsonIgnore
    private Set<OrderItem> orderItems = new LinkedHashSet<>();

}