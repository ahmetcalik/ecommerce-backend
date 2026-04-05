package com.project.ecommerce_backend.entities.concretes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Platformdaki satın alma işlemlerinin ana yönetim ve takip merkezini temsil eden varlık sınıfıdır.
 * Müşteri bilgileri, teslimat detayları ve ürün kalemleri arasındaki ilişkileri koordine eder; siparişin mali toplamını, güncel durumunu (hazırlanıyor, kargolandı vb.) ve ilişkili fatura bilgilerini tek bir çatı altında toplayarak tüm satış döngüsünü yönetir.
 */
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_customer_id", columnList = "customer_id"),
        @Index(name = "idx_orders_shipping_address_id", columnList = "shipping_address_id"),
        @Index(name = "idx_orders_shipping_method_id", columnList = "shipping_method_id"),
        @Index(name = "idx_orders_order_status_id", columnList = "order_status_id")
})
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Customer customer;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipping_address_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Address shippingAddress;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipping_method_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ShippingMethod shippingMethod;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_status_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private OrderStatus orderStatus;

    @NotNull
    @Column(name = "order_date", nullable = false)
    private OffsetDateTime orderDate;

    @NotNull
    @Column(name = "order_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal orderTotal;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "order")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Invoice invoice;

    @OneToMany(mappedBy = "order")
    @JsonIgnore
    private Set<OrderItem> orderItems = new LinkedHashSet<>();
}