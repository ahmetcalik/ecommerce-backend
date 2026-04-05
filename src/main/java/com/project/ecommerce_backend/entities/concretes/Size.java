package com.project.ecommerce_backend.entities.concretes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Ürün varyantlarının beden veya ölçü özelliklerini tanımlayan ve merkezi bir havuzda yönetilmesini sağlayan varlık sınıfıdır.
 * Ürün kalemleri (ProductItem) ile ilişkilendirilerek, aynı ana ürünün farklı boyut seçeneklerinin stok ve satış süreçlerinde ayrıştırılmasını sağlar; tekil isim kısıtıyla veri tekrarını önler.
 */
@Getter
@Setter
@Entity
@Table(name = "size", uniqueConstraints = {
        @UniqueConstraint(name = "size_size_name_key", columnNames = {"size_name"})
})
public class Size extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @jakarta.validation.constraints.Size(max = 50)
    @NotNull
    @Column(name = "size_name", nullable = false, length = 50)
    private String sizeName;

    @OneToMany(mappedBy = "size")
    @JsonIgnore
    private Set<ProductItem> productItems = new LinkedHashSet<>();

}