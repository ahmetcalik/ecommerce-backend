package com.project.ecommerce_backend.entities.concretes;

import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Ödeme süreçlerinde kart ailelerine göre sunulacak taksit seçeneklerini ve maliyet oranlarını yöneten varlık sınıfıdır.
 * Farklı banka kartları için özelleştirilmiş taksit sayılarını ve bu taksitlere uygulanan faiz oranlarını saklayarak; ödeme aşamasında sepet toplamının doğru maliyetlerle taksitlendirilmesini ve finansal tutarlılığı sağlar.
 */
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "installment_option")
public class InstallmentOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "card_family_name", nullable = false)
    private String cardFamilyName;

    @NotNull
    @Min(1)
    @Column(name = "number_of_installments", nullable = false)
    private Integer numberOfInstallments;

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}