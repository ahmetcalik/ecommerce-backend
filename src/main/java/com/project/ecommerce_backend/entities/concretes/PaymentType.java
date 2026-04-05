package com.project.ecommerce_backend.entities.concretes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import com.project.ecommerce_backend.entities.enums.PaymentTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Platformda desteklenen ödeme yöntemlerinin kategorizasyonunu ve yönetimini sağlayan varlık sınıfıdır.
 * Ödeme türlerini enum bazlı sabitlerle veritabanı seviyesinde tekilleştirerek, müşteri ödeme yöntemlerinin doğru finansal protokollerle eşleştirilmesine ve ödeme süreçlerinin tip güvenli bir şekilde yürütülmesine olanak tanır.
 */
@Getter
@Setter
@Entity
@Table(name = "payment_type", uniqueConstraints = {
        @UniqueConstraint(name = "payment_type_type_name_key", columnNames = {"type_name"})
})
public class PaymentType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_name", nullable = false, unique = true)
    private PaymentTypeEnum typeName;

    @OneToMany(mappedBy = "paymentType")
    @JsonIgnore
    private Set<PaymentMethod> paymentMethods = new LinkedHashSet<>();

}