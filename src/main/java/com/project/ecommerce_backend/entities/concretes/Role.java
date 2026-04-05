package com.project.ecommerce_backend.entities.concretes;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Uygulama genelindeki yetkilendirme ve erişim kontrolü stratejilerini tanımlayan varlık sınıfıdır.
 * Spring Security katmanında kullanıcıların rollerine göre işlem yapmasını sağlayan "Role-Based Access Control" yapısına veri sağlar ve her rolün sistemde benzersiz bir isimle tanımlanmasını garanti eder.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "role")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;
}

