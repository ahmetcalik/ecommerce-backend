package com.project.ecommerce_backend.business.dtos.responses.invoice;

import com.project.ecommerce_backend.entities.enums.InvoiceStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class ListUserInvoicesResponse {
    private Long id; // Fatura ID
    private String invoiceNumber; // Fatura Numarası
    private OffsetDateTime invoiceDate; // Fatura Tarihi
    private BigDecimal grandTotal; // Genel Toplam
    private InvoiceStatus status; // Faturanın Durumu (ÖDENDİ, İPTAL EDİLDİ vb.)
    private Long orderId; // İlişkili Siparişin ID'si
}