package com.project.ecommerce_backend.entities.concretes;

import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import com.project.ecommerce_backend.entities.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;

/**
 * Müşterilerin periyodik üyelik süreçlerini ve tekrarlayan ödeme döngülerini yöneten varlık sınıfıdır.
 * Kayıtlı bir müşteri ile belirli bir ödeme yöntemini abonelik statüsü altında ilişkilendirerek; hizmet başlangıç, bitiş ve bir sonraki faturalandırma tarihlerinin takibini ve otomatik ödeme süreçlerinin yürütülmesini sağlar.
 */
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "subscription")
public class Subscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubscriptionStatus status;

    @Column(name = "start_date", nullable = false)
    private OffsetDateTime startDate;

    @Column(name = "next_billing_date")
    private OffsetDateTime nextBillingDate;

    @Column(name = "end_date")
    private OffsetDateTime endDate;
}