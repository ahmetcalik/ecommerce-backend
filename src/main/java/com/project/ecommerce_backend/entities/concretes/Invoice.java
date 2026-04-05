package com.project.ecommerce_backend.entities.concretes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import com.project.ecommerce_backend.entities.enums.InvoiceStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Siparişlerin mali ve yasal kayıtlarını tutan fatura varlık sınıfıdır.
 * Sipariş toplamı, vergi dilimleri, kargo maliyetleri ve taksitli işlemlere ait faiz tutarlarını detaylandırarak; hem müşteri için şeffaf bir ödeme özeti sunar hem de işletmenin muhasebe süreçleri için gerekli olan resmi veri tabanını oluşturur.
 */
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "invoice", indexes = {
        @Index(name = "idx_invoice_order_id", columnList = "order_id"),
        @Index(name = "idx_invoice_payment_method_id", columnList = "payment_method_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "invoice_order_id_key", columnNames = {"order_id"})
})
public class Invoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotBlank(message = "Fatura numarası boş olamaz.")
    @Size(max = 255)
    @Column(name = "invoice_number", unique = true, nullable = false)
    private String invoiceNumber;

    @Column(name = "sub_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal subTotal;

    @Column(name = "vat_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "grand_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal grandTotal;

    @NotNull
    @Column(name = "invoice_date", nullable = false)
    private OffsetDateTime invoiceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status;

    @NotNull
    @Min(1)
    @Column(name = "installment_count", nullable = false)
    private Integer installmentCount;

    @Column(name = "interest_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal interestAmount;

    @Column(name = "shipping_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingFee;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "invoice"})
    private Order order;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_method_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private PaymentMethod paymentMethod;

    @PrePersist
    protected void onCreate() {
        if (this.invoiceDate == null) {
            this.invoiceDate = OffsetDateTime.now();
        }
        if (this.invoiceNumber == null && this.order != null) {
            String timestamp = DateTimeFormatter.ofPattern("ddMMyyyyHHmmss").format(this.invoiceDate);
            this.invoiceNumber = String.format("INV-%s-%d", timestamp, this.order.getId());
        }
    }
}