package com.project.ecommerce_backend.entities.concretes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Ürün varyantlarının renk özelliklerini tanımlayan ve merkezi bir havuzda yönetilmesini sağlayan varlık sınıfıdır.
 * Ürün kalemleri (ProductItem) ile ilişkilendirilerek, aynı ana ürünün farklı renk seçeneklerinin stok ve satış süreçlerinde ayrıştırılmasını sağlar; tekil isim kısıtıyla veri tekrarını önler.
 */
@Getter
@Setter
@Entity
@Table(name = "colour", uniqueConstraints = {
        @UniqueConstraint(name = "colour_colour_name_key", columnNames = {"colour_name"})
})
public class Colour extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 50)
    @NotNull
    @Column(name = "colour_name", nullable = false, length = 50)
    private String colourName;

    @OneToMany(mappedBy = "colour")
    @JsonIgnore
    private Set<ProductItem> productItems = new LinkedHashSet<>();

}