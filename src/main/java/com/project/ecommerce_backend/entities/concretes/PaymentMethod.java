package com.project.ecommerce_backend.entities.concretes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Müşterilere ait ödeme araçlarının ve kart saklama verilerinin güvenli bir şekilde yönetilmesini sağlayan varlık sınıfıdır.
 * Ödeme kuruluşları üzerinden alınan anahtar değerleri kullanarak hassas verileri doğrudan saklamadan işlem yapılmasını mümkün kılar ve müşterilerin sonraki alışverişlerinde kayıtlı ödeme yöntemlerini kullanmasına olanak tanır.
 */
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment_method")
public class PaymentMethod extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_type_id", nullable = false)
    private PaymentType paymentType;

    @NotNull
    @Column(name = "card_holder_name", nullable = false)
    private String cardHolderName;

    @NotNull
    @Column(name = "last_four_digits", nullable = false, length = 4)
    private String lastFourDigits;

    @NotNull
    @Column(name = "card_family", nullable = false)
    private String cardFamily;

    @NotNull
    @Column(name = "expiry_month", nullable = false)
    private Integer expiryMonth;

    @NotNull
    @Column(name = "expiry_year", nullable = false)
    private Integer expiryYear;

    @NotNull
    @Column(name = "card_token", nullable = false, unique = true)
    private String cardToken;

    @OneToMany(mappedBy = "paymentMethod")
    @JsonIgnore
    private Set<Invoice> invoices = new LinkedHashSet<>();
}