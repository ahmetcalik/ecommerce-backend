package com.project.ecommerce_backend.entities.concretes;

import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Alışveriş sürecindeki ürünlerin geçici olarak biriktirildiği ve yönetildiği sepet varlık sınıfıdır.
 * Kayıtlı müşteriler için Customer ilişkisi, anonim kullanıcılar için ise sessionId üzerinden sepet takibi yaparak; sepet içeriğinin (ShoppingCartItem) merkezi olarak saklanmasını, güncellenmesini ve siparişe dönüştürülmesini koordine eder.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shopping_cart")
public class ShoppingCart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", unique = true)
    private Customer customer;

    @Column(name = "session_id", unique = true)
    private UUID sessionId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ShoppingCartItem> cartItems = new LinkedHashSet<>();
}

