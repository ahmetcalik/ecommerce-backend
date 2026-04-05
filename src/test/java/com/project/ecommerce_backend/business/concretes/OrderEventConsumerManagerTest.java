package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.*;
import com.project.ecommerce_backend.business.dtos.events.OrderCreatedEvent;
import com.project.ecommerce_backend.business.dtos.responses.installment.InstallmentOptionResponse;
import com.project.ecommerce_backend.entities.concretes.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerManagerTest {

    @Mock private ProductService productService;
    @Mock private InvoiceService invoiceService;
    @Mock private OrderService orderService;
    @Mock private PaymentMethodService paymentMethodService;
    @Mock private InstallmentService installmentService;

    @InjectMocks
    private OrderEventConsumerManager consumerManager;

    // 1. handleOrderCreatedEventForStock TESTS
    @Test
    void handleOrderCreatedEventForStock_whenSuccess_shouldReduceStock() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(1L);
        OrderCreatedEvent.OrderItemDetail item = new OrderCreatedEvent.OrderItemDetail(10L, 2);
        event.setItems(List.of(item));

        doNothing().when(productService).checkAndReduceStock(10L, 2);

        consumerManager.handleOrderCreatedEventForStock(event);

        verify(productService).checkAndReduceStock(10L, 2);
    }

    @Test
    void handleOrderCreatedEventForStock_whenError_shouldThrowRuntimeException() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(1L);
        OrderCreatedEvent.OrderItemDetail item = new OrderCreatedEvent.OrderItemDetail(10L, 2);
        event.setItems(List.of(item));

        doThrow(new RuntimeException("Stock Error")).when(productService).checkAndReduceStock(10L, 2);

        assertThrows(RuntimeException.class, () -> consumerManager.handleOrderCreatedEventForStock(event));
    }

    // 2. handleOrderCreatedEventForInvoice TESTS
    @Test
    void handleOrderCreatedEventForInvoice_whenSuccess_shouldCreateInvoice() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(1L);
        event.setCustomerId(1L);
        event.setPaymentMethodId(1L);
        event.setInstallmentCount(1);

        Order order = new Order();
        order.setId(1L);
        ShippingMethod shippingMethod = new ShippingMethod();
        order.setShippingMethod(shippingMethod);
        
        OrderItem orderItem = new OrderItem();
        orderItem.setPriceAtOrder(BigDecimal.TEN);
        orderItem.setQuantity(1);
        orderItem.setVatRate(BigDecimal.ZERO);
        order.setOrderItems(Set.of(orderItem));

        PaymentMethod paymentMethod = new PaymentMethod();

        when(orderService.findByIdAsEntityWithDetails(1L)).thenReturn(order);
        when(paymentMethodService.getByIdAndCustomerId(1L, 1L)).thenReturn(paymentMethod);

        consumerManager.handleOrderCreatedEventForInvoice(event);

        verify(invoiceService).createInvoiceForOrder(
                eq(order),
                eq(paymentMethod),
                any(BigDecimal.class), // SubTotal
                any(BigDecimal.class), // Vat
                eq(BigDecimal.ZERO),   // Interest (1 installment)
                eq(1),
                eq(shippingMethod)
        );
    }

    @Test
    void handleOrderCreatedEventForInvoice_whenInstallment_shouldCalculateInterest() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(1L);
        event.setCustomerId(1L);
        event.setPaymentMethodId(1L);
        event.setInstallmentCount(3); // Installment

        Order order = new Order();
        order.setId(1L);
        ShippingMethod shippingMethod = new ShippingMethod();
        order.setShippingMethod(shippingMethod);

        OrderItem orderItem = new OrderItem();
        orderItem.setPriceAtOrder(BigDecimal.TEN);
        orderItem.setQuantity(1);
        orderItem.setVatRate(BigDecimal.ZERO);
        order.setOrderItems(Set.of(orderItem));

        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setCardFamily("Visa");

        InstallmentOptionResponse option = new InstallmentOptionResponse();
        option.setTotalAmount(BigDecimal.valueOf(12)); // 10 + 2 interest

        when(orderService.findByIdAsEntityWithDetails(1L)).thenReturn(order);
        when(paymentMethodService.getByIdAndCustomerId(1L, 1L)).thenReturn(paymentMethod);
        when(installmentService.getSpecificOption(any(), any(), eq(3))).thenReturn(option);

        consumerManager.handleOrderCreatedEventForInvoice(event);

        verify(invoiceService).createInvoiceForOrder(
                eq(order),
                eq(paymentMethod),
                any(BigDecimal.class),
                any(BigDecimal.class),
                eq(BigDecimal.valueOf(2)), // Interest amount
                eq(3),
                eq(shippingMethod)
        );
    }

    @Test
    void handleOrderCreatedEventForInvoice_whenError_shouldThrowRuntimeException() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(1L);

        when(orderService.findByIdAsEntityWithDetails(1L)).thenThrow(new RuntimeException("DB Error"));

        assertThrows(RuntimeException.class, () -> consumerManager.handleOrderCreatedEventForInvoice(event));
    }
}
