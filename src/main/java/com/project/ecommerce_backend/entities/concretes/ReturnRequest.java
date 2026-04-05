package com.project.ecommerce_backend.entities.concretes;

import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import com.project.ecommerce_backend.entities.enums.ReturnReasonEnum;
import com.project.ecommerce_backend.entities.enums.ReturnStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Müşterilerin satın aldıkları ürünler için başlattıkları iade süreçlerini yöneten varlık sınıfıdır.
 * Belirli bir sipariş kalemi ile iade nedeni ve güncel değerlendirme durumunu ilişkilendirerek; iade taleplerinin incelenmesini, onaylanmasını veya reddedilmesini sağlayan operasyonel iş akışını takip eder.
 */
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "return_request", indexes = {
        @Index(name = "idx_return_request_order_id", columnList = "order_id"),
        @Index(name = "idx_return_request_order_item_id", columnList = "order_item_id"),
        @Index(name = "idx_return_request_status", columnList = "status")
})
public class ReturnRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private ReturnReasonEnum reason;

    @Column(name = "custom_reason")
    private String customReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReturnStatusEnum status;
}