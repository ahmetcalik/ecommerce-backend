package com.project.ecommerce_backend.entities.concretes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

/**
 * Müşteriler ve adresler arasındaki ilişkiyi yöneten ve bu ilişkiye iş mantığı detayları ekleyen bağlayıcı varlık sınıfıdır.
 * Kompozit anahtar yapısı sayesinde her müşterinin adres eşleşmelerini tekilleştirir; adreslerin "sevkiyat" veya "fatura" adresi olarak rollerini tanımlayarak sipariş süreçlerinde doğru lokasyonun otomatik seçilmesine olanak tanır.
 */
@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "customer_address", indexes = {
        @Index(name = "idx_customer_address_customer_id", columnList = "customer_id"),
        @Index(name = "idx_customer_address_address_id", columnList = "address_id")
})
public class CustomerAddress extends BaseEntity {

    @EmbeddedId
    private CustomerAddressId id;

    @MapsId("customerId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    @MapsId("addressId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "address_id", nullable = false)
    @JsonIgnore
    private Address address;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_shipping_address", nullable = false)
    private Boolean isShippingAddress = false;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_billing_address", nullable = false)
    private Boolean isBillingAddress = false;
}