package com.project.ecommerce_backend.entities.concretes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Siparişlerin teslimat yöntemlerini ve bu yöntemlere ait birim fiyatları tanımlayan varlık sınıfıdır.
 * Farklı sevkiyat seçeneklerini merkezi bir havuzda toplayarak; sipariş oluşturma aşamasında kargo maliyetlerinin doğru hesaplanmasını ve lojistik tercihlerin sipariş geçmişiyle ilişkilendirilmesini sağlar.
 */
@Getter
@Setter
@Entity
@Table(name = "shipping_method", uniqueConstraints = {
        @UniqueConstraint(name = "shipping_method_name_key", columnNames = {"name"})
})
public class ShippingMethod extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 100)
    @NotNull
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotNull
    @Column(name = "shipping_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingPrice;

    @OneToMany(mappedBy = "shippingMethod")
    @JsonIgnore
    private Set<Order> orders = new LinkedHashSet<>();

}