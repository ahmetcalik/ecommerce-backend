package com.project.ecommerce_backend.entities.concretes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Sistem genelinde kullanılan fiziksel konum bilgilerini merkezi olarak yöneten varlık sınıfıdır.
 * Müşteriler, tedarikçiler ve sipariş sevkiyatları için ortak bir adres havuzu oluşturarak veri tekrarını önler; ülke bazlı indeksleme ve ilişkisel kısıtlarla sevkiyat süreçlerinin doğruluğunu garanti altına alır.
 */
@Getter
@Setter
@Entity
@Table(name = "address", indexes = {
        @Index(name = "idx_address_country_id", columnList = "country_id")
})
public class Address extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 100)
    @NotNull
    @Column(name = "title", nullable = false)
    private String title;

    @Size(max = 255)
    @NotNull
    @Column(name = "address_line_1", nullable = false)
    private String addressLine1;

    @Size(max = 255)
    @Column(name = "address_line_2")
    private String addressLine2;

    @Size(max = 255)
    @NotNull
    @Column(name = "city", nullable = false)
    private String city;

    @Size(max = 10)
    @NotNull
    @Column(name = "postal_code", nullable = false, length = 10)
    private String postalCode;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "country_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Country country;

    @OneToMany(mappedBy = "address")
    @JsonIgnore
    private Set<CustomerAddress> customerAddresses = new LinkedHashSet<>();

    @OneToMany(mappedBy = "shippingAddress")
    @JsonIgnore
    private Set<Order> orders = new LinkedHashSet<>();

    @OneToMany(mappedBy = "address")
    @JsonIgnore
    private Set<Supplier> suppliers = new LinkedHashSet<>();

}