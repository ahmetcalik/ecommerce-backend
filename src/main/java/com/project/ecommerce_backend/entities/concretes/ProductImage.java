package com.project.ecommerce_backend.entities.concretes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

/**
 * Ürünlere ait görsel içeriklerin dosya yollarını ve gösterim tercihlerini yöneten varlık sınıfıdır.
 * Ürünler ile kurduğu çok-bir ilişki sayesinde her ürüne ait zengin bir görsel galeri oluşturulmasını sağlar; ana görsel işareti üzerinden vitrin ve arama sonuçlarında kullanılacak öncelikli medyanın belirlenmesini mümkün kılar.
 */
@Getter
@Setter
@Entity
@Table(name = "product_image", indexes = {
        @Index(name = "idx_product_image_product_id", columnList = "product_id")
})
public class ProductImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product product;

    @Size(max = 255)
    @NotNull
    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_main_image", nullable = false)
    private Boolean isMainImage = false;
}