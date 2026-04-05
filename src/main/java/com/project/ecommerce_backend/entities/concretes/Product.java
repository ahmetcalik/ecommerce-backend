package com.project.ecommerce_backend.entities.concretes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Platformdaki ürünlerin kimlik, tedarikçi ve vergi bilgilerini içeren temel varlık sınıfıdır.
 * Ürün görselleri, varyantlar, kategoriler ve müşteri yorumları gibi ilişkili tüm verileri tek bir çatı altında toplayarak ürün kataloğunun yönetimini sağlar ve aktiflik kontrolü üzerinden listeleme süreçlerini optimize eder.
 */
@Getter
@Setter
@Entity
@SQLRestriction("is_active = true")
@Table(name = "product", indexes = {
        @Index(name = "idx_product_supplier_id", columnList = "supplier_id"),
        @Index(name = "idx_product_is_active", columnList = "is_active")
})
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Supplier supplier;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    private Set<CustomerReview> customerReviews = new LinkedHashSet<>();

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    private Set<ProductCategory> productCategories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    private Set<ProductImage> productImages = new LinkedHashSet<>();

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    private Set<ProductItem> productItems = new LinkedHashSet<>();

    @NotNull
    @Column(name = "vat_rate", nullable = false, precision = 4, scale = 2)
    private BigDecimal vatRate;

}