package com.project.ecommerce_backend.entities.concretes;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Ödeme süreçlerinde kart doğrulama ve taksit seçeneklerinin belirlenmesi için kullanılan Banka Kimlik Numarası (BIN) varlığıdır.
 * Kart numarasının ilk sekiz hanesi üzerinden ilgili kartın hangi bankaya ve kart ailesine ait olduğunu tespit ederek, iş mantığı katmanında doğru taksit oranlarının ve kampanyaların sunulmasını sağlar.
 */
@Data
@Entity
@Table(name = "bin_numbers")
public class BinNumber {
    @Id
    @Column(name = "bin_number", length = 8)
    private String id;

    @Column(name = "card_family_name", nullable = false)
    private String cardFamilyName;

    @Column(name = "bank_name", nullable = false)
    private String bankName;
}