package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.OrderStatusService;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.entities.concretes.OrderStatus;
import com.project.ecommerce_backend.entities.enums.OrderStatusEnum;
import com.project.ecommerce_backend.repositories.abstracts.OrderStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStatusManagerTest {

    @Mock
    private OrderStatusRepository orderStatusRepository;

    @Mock
    private MessageService messageService;

    @Mock
    private OrderStatusService self;

    private OrderStatusManager orderStatusManager;

    @BeforeEach
    void setUp() {
        orderStatusManager = new OrderStatusManager(orderStatusRepository, messageService, self);
    }

    @Test
    void getInitialStatus_shouldReturnPreparing() {
        OrderStatus status = new OrderStatus();
        status.setStatusName(OrderStatusEnum.PREPARING);

        when(self.findStatusByName(OrderStatusEnum.PREPARING)).thenReturn(status);

        OrderStatus result = orderStatusManager.getInitialStatus();

        assertEquals(OrderStatusEnum.PREPARING, result.getStatusName());
    }

    @Test
    void getShippedStatus_shouldReturnShipped() {
        OrderStatus status = new OrderStatus();
        status.setStatusName(OrderStatusEnum.SHIPPED);

        when(self.findStatusByName(OrderStatusEnum.SHIPPED)).thenReturn(status);

        OrderStatus result = orderStatusManager.getShippedStatus();

        assertEquals(OrderStatusEnum.SHIPPED, result.getStatusName());
    }

    @Test
    void getDeliveredStatus_shouldReturnDelivered() {
        OrderStatus status = new OrderStatus();
        status.setStatusName(OrderStatusEnum.DELIVERED);

        when(self.findStatusByName(OrderStatusEnum.DELIVERED)).thenReturn(status);

        OrderStatus result = orderStatusManager.getDeliveredStatus();

        assertEquals(OrderStatusEnum.DELIVERED, result.getStatusName());
    }

    @Test
    void getCancelledStatus_shouldReturnCancelled() {
        OrderStatus status = new OrderStatus();
        status.setStatusName(OrderStatusEnum.CANCELLED);

        when(self.findStatusByName(OrderStatusEnum.CANCELLED)).thenReturn(status);

        OrderStatus result = orderStatusManager.getCancelledStatus();

        assertEquals(OrderStatusEnum.CANCELLED, result.getStatusName());
    }

    @Test
    void findStatusByName_whenFound_shouldReturnStatus() {
        OrderStatus status = new OrderStatus();
        status.setStatusName(OrderStatusEnum.PREPARING);
        when(orderStatusRepository.findByStatusName(OrderStatusEnum.PREPARING)).thenReturn(Optional.of(status));

        OrderStatus result = orderStatusManager.findStatusByName(OrderStatusEnum.PREPARING);

        assertEquals(status, result);
    }

    @Test
    void findStatusByName_whenMissing_shouldThrowNotFoundException() {
        when(orderStatusRepository.findByStatusName(any())).thenReturn(Optional.empty());
        when(messageService.getMessageWithParams(any(), any())).thenReturn("Error");

        assertThrows(NotFoundException.class, () -> orderStatusManager.findStatusByName(OrderStatusEnum.PREPARING));
    }
}
