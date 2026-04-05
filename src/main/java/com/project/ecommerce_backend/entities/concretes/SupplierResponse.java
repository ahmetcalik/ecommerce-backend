package com.project.ecommerce_backend.entities.concretes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

/**
 * Tedarikçilerin müşteri yorumlarına verdikleri kurumsal geri bildirimleri yöneten varlık sınıfıdır.
 * Belirli bir müşteri yorumu (CustomerReview) ile ilgili tedarikçiyi (Supplier) ilişkilendirerek; verilen yanıtın içeriğini ve tarihini saklar, böylece platform üzerindeki satıcı-müşteri etkileşiminin profesyonel bir şekilde izlenmesini ve sergilenmesini sağlar.
 */
@Getter
@Setter
@Entity
@Table(name = "supplier_response", indexes = {
        @Index(name = "idx_supplier_response_review_id", columnList = "customer_review_id"),
        @Index(name = "idx_supplier_response_supplier_id", columnList = "supplier_id")
})
public class SupplierResponse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_review_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private CustomerReview customerReview;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Supplier supplier;

    @NotNull
    @Column(name = "response_text", nullable = false, length = Integer.MAX_VALUE)
    private String responseText;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "response_date", nullable = false)
    private OffsetDateTime responseDate;
}