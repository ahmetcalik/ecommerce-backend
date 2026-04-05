package com.project.ecommerce_backend.business.helpers;

import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.BusinessException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.security.details.CustomerDetails;
import com.project.ecommerce_backend.entities.concretes.Invoice;
import com.project.ecommerce_backend.entities.concretes.Order;
import com.project.ecommerce_backend.repositories.abstracts.InvoiceRepository;
import com.project.ecommerce_backend.repositories.abstracts.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Cache Eviction ve Cacheable metotları için gerekli olan karmaşık SpEL
 * anahtar hesaplamalarını (DB sorgusu veya Security Context erişimi gerektiren)
 * güvenli bir şekilde sunan yardımcı sınıftır.
 * Amacı, OrderManager ve InvoiceManager sınıflarındaki yardımcı metotların
 * tekrar private olmasını sağlamaktır.
 */
@Component
@RequiredArgsConstructor
public class CacheHelper {

    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final MessageService messageService;


    /**
     * Cache key'leri için kimliği doğrulanmış kullanıcının ID'sini döndürür.
     */
    public Long getAuthenticatedUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomerDetails) {
            return ((CustomerDetails) principal).getId();
        }

        throw new BusinessException(messageService.getMessage(messageService.getMessage(
                Messages.Auth.AUTHENTICATION_PRINCIPAL_INVALID)));
    }

    /**
     * SpEL'in Order ID'si üzerinden Customer ID'sine erişmesi için kullanılır.
     * OrderManager'daki findOrderByIdOrThrow metodunun işlevini üstlenir.
     */
    public Long getOrderCustomerId(Long orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);

        return orderOpt
                .map(order -> order.getCustomer().getId())
                .orElse(null);
    }

    /**
     * SpEL'in Invoice ID'si üzerinden Customer ID'sine erişmesi için kullanılır.
     * InvoiceManager'daki getInvoiceCustomerId metodunun işlevini üstlenir.
     */
    public Long getInvoiceCustomerId(Long invoiceId) {
        Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId);

        return invoiceOpt
                .map(invoice -> invoice.getOrder().getCustomer().getId())
                .orElse(null);
    }
}