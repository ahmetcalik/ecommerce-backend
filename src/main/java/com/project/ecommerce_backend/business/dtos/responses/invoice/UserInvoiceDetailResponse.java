package com.project.ecommerce_backend.business.dtos.responses.invoice;

import com.project.ecommerce_backend.business.dtos.responses.order.OrderItemResponse;
import com.project.ecommerce_backend.entities.enums.InvoiceStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class UserInvoiceDetailResponse {
    private Long id;
    private String invoiceNumber;
    private OffsetDateTime invoiceDate;
    private InvoiceStatus status;

    // Finansal Detaylar
    private BigDecimal subTotal;
    private BigDecimal vatAmount;
    private BigDecimal interestAmount;
    private BigDecimal shippingFee;
    private BigDecimal grandTotal;
    private Integer installmentCount;

    // İlişkili Sipariş Bilgileri
    private Long orderId;
    private OffsetDateTime orderDate;
    private List<OrderItemResponse> orderItems;

    // Ödeme Bilgileri
    private String maskedCardNumber;
    private String cardFamily;
}