package com.project.ecommerce_backend.entities.concretes;

import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Siparişlerin sevkiyat süreçlerini yürüten kargo firmalarını tanımlayan varlık sınıfıdır.
 * Sistemdeki aktif kargo seçeneklerini yöneterek, sipariş oluşturma aşamasında kullanıcıya sunulacak lojistik sağlayıcıların ve bu sağlayıcılara ait durum bilgilerinin merkezi olarak kontrol edilmesini sağlar.
 */
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "carrier")
public class Carrier extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name; // Örn: "Yurtiçi Kargo"

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}