package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.dtos.responses.invoice.ListUserInvoicesResponse;
import com.project.ecommerce_backend.business.dtos.responses.invoice.UserInvoiceDetailResponse;
import com.project.ecommerce_backend.business.helpers.CacheHelper;
import com.project.ecommerce_backend.core.exceptions.types.BusinessException;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.utils.mapper.ModelMapperService;
import com.project.ecommerce_backend.entities.concretes.Invoice;
import com.project.ecommerce_backend.entities.concretes.Order;
import com.project.ecommerce_backend.entities.concretes.PaymentMethod;
import com.project.ecommerce_backend.entities.concretes.ShippingMethod;
import com.project.ecommerce_backend.entities.enums.InvoiceStatus;
import com.project.ecommerce_backend.repositories.abstracts.InvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceManagerTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private ModelMapperService modelMapperService;
    @Mock private MessageService messageService;
    @Mock private CacheHelper cacheHelper;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private InvoiceManager invoiceManager;

    private Long customerId = 1L;
    private Long invoiceId = 10L;

    // 1. createInvoiceForOrder TESTS
    @Test
    void createInvoiceForOrder_shouldCreateInvoice() {
        Order order = new Order();
        order.setOrderTotal(BigDecimal.TEN);
        PaymentMethod paymentMethod = new PaymentMethod();
        ShippingMethod shippingMethod = new ShippingMethod();
        shippingMethod.setShippingPrice(BigDecimal.ONE);

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArguments()[0]);

        var result = invoiceManager.createInvoiceForOrder(order, paymentMethod, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, 1, shippingMethod);

        assertNotNull(result);
        assertEquals(InvoiceStatus.PAID, result.getStatus());
        verify(invoiceRepository).save(any(Invoice.class));
    }

    // 2. getAllMyInvoices TESTS
    @Test
    void getAllMyInvoices_shouldReturnList() {
        Pageable pageable = Pageable.unpaged();
        Invoice invoice = new Invoice();
        Slice<Invoice> slice = new PageImpl<>(Collections.singletonList(invoice));

        when(cacheHelper.getAuthenticatedUserId()).thenReturn(customerId);
        when(invoiceRepository.findSliceByCustomerId(customerId, pageable)).thenReturn(slice);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(invoice, ListUserInvoicesResponse.class)).thenReturn(new ListUserInvoicesResponse());

        var result = invoiceManager.getAllMyInvoices(pageable);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
    }

    // 3. getMyInvoiceById TESTS
    @Test
    void getMyInvoiceById_whenAuthorized_shouldReturnDetail() {
        Invoice invoice = new Invoice();
        when(cacheHelper.getAuthenticatedUserId()).thenReturn(customerId);
        when(invoiceRepository.findByIdAndCustomerIdWithDetails(invoiceId, customerId)).thenReturn(Optional.of(invoice));
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(invoice, UserInvoiceDetailResponse.class)).thenReturn(new UserInvoiceDetailResponse());

        var result = invoiceManager.getMyInvoiceById(invoiceId);

        assertTrue(result.isSuccess());
    }

    @Test
    void getMyInvoiceById_whenUnauthorized_shouldThrowNotFoundException() {
        when(cacheHelper.getAuthenticatedUserId()).thenReturn(customerId);
        when(invoiceRepository.findByIdAndCustomerIdWithDetails(invoiceId, customerId)).thenReturn(Optional.empty());
        when(messageService.getMessage(any())).thenReturn("Error");

        assertThrows(NotFoundException.class, () -> invoiceManager.getMyInvoiceById(invoiceId));
    }

    // 4. cancelInvoice TESTS
    @Test
    void cancelInvoice_whenPaid_shouldCancel() {
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.PAID);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        var result = invoiceManager.cancelInvoice(invoiceId);

        assertTrue(result.isSuccess());
        assertEquals(InvoiceStatus.CANCELLED, invoice.getStatus());
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void cancelInvoice_whenAlreadyCancelled_shouldThrowBusinessException() {
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.CANCELLED);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(messageService.getMessageWithParams(any(), any())).thenReturn("Error");

        assertThrows(BusinessException.class, () -> invoiceManager.cancelInvoice(invoiceId));
    }

    // 5. markInvoiceAsRefunded TESTS
    @Test
    void markInvoiceAsRefunded_whenPaid_shouldRefund() {
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.PAID);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        var result = invoiceManager.markInvoiceAsRefunded(invoiceId);

        assertTrue(result.isSuccess());
        assertEquals(InvoiceStatus.REFUNDED, invoice.getStatus());
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void markInvoiceAsRefunded_whenNotPaid_shouldThrowBusinessException() {
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.CANCELLED);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(messageService.getMessage(any())).thenReturn("Error");

        assertThrows(BusinessException.class, () -> invoiceManager.markInvoiceAsRefunded(invoiceId));
    }
}
