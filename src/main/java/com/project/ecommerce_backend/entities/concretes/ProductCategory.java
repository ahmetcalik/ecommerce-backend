package com.project.ecommerce_backend.entities.concretes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * Ürünler ve kategoriler arasındaki çok-çok ilişkiyi normalize eden bağlayıcı varlık sınıfıdır.
 * Kompozit anahtar yapısı üzerinden her ürünün ilgili kategorilerle olan eşleşmesini tekilleştirir; ürünlerin hiyerarşik kategori ağacı içerisinde farklı dallarda konumlandırılmasına ve doğru listeleme sonuçlarının üretilmesine imkan tanır.
 */
@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "product_category", indexes = {
        @Index(name = "idx_product_category_product_id", columnList = "product_id"),
        @Index(name = "idx_product_category_category_id", columnList = "category_id")
})
public class ProductCategory extends BaseEntity {

    @EmbeddedId
    private ProductCategoryId id;

    @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;

    @MapsId("categoryId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    @JsonIgnore
    private Category category;

}