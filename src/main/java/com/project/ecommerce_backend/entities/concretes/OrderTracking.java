package com.project.ecommerce_backend.entities.concretes;

import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Kargolanan siparişlerin lojistik takip süreçlerini yöneten varlık sınıfıdır.
 * Belirli bir siparişi, ilgili kargo firması ve benzersiz takip numarasıyla eşleştirerek; sevkiyat aşamasındaki ürünlerin anlık durumunun hem sistem hem de müşteri tarafından izlenebilmesini sağlar.
 */
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_tracking")
public class OrderTracking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "tracking_number", nullable = false, unique = true)
    private String trackingNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_id", nullable = false)
    private Carrier carrier;
}