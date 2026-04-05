package com.project.ecommerce_backend.entities.concretes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import com.project.ecommerce_backend.entities.enums.OrderStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Siparişlerin yaşam döngüsü içerisindeki güncel aşamalarını tanımlayan ve yöneten durum varlığıdır.
 * Enum tabanlı durum isimlerini veritabanı seviyesinde standartlaştırarak; operasyonel süreçlerin takibini, durum bazlı raporlamaları ve sipariş akış yönetimini (State Management) merkezi bir yapı üzerinden yürütür.
 */
@Getter
@Setter
@Entity
@Table(name = "order_status", uniqueConstraints = {
        @UniqueConstraint(name = "order_status_status_name_key", columnNames = {"status_name"})
})
public class OrderStatus extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_name", nullable = false, unique = true)
    private OrderStatusEnum statusName;

    @OneToMany(mappedBy = "orderStatus")
    @JsonIgnore
    private Set<Order> orders = new LinkedHashSet<>();

}