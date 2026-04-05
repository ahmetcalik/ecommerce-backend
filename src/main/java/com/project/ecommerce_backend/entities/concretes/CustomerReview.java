package com.project.ecommerce_backend.entities.concretes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Müşterilerin satın aldıkları ürünler hakkında geri bildirimde bulunmalarını sağlayan değerlendirme ve yorum varlığıdır.
 * Ürün ve müşteri arasında tekil bir ilişki kurarak mükerrer yorumları önler; puanlama ve yorum sistemi üzerinden ürün kalitesini izlemeye yardımcı olurken, tedarikçilerin bu yorumlara yanıt vermesine imkan tanıyan ilişkisel bir yapı sunar.
 */
@Getter
@Setter
@Entity
@Table(name = "customer_review", indexes = {
        @Index(name = "idx_customer_review_product_id", columnList = "product_id"),
        @Index(name = "idx_customer_review_customer_id", columnList = "customer_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "customer_review_product_id_customer_id_key", columnNames = {"product_id", "customer_id"})
})
public class CustomerReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    @NotNull
    @Column(name = "rating_value", nullable = false)
    private Integer ratingValue;

    @Column(name = "comment", length = Integer.MAX_VALUE)
    private String comment;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "review_date", nullable = false)
    private OffsetDateTime reviewDate;

    @OneToMany(mappedBy = "customerReview")
    @JsonIgnore
    private Set<SupplierResponse> supplierResponses = new LinkedHashSet<>();

}