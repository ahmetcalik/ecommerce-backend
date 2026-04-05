package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.entities.concretes.PaymentType;
import com.project.ecommerce_backend.entities.enums.PaymentTypeEnum;
import com.project.ecommerce_backend.repositories.abstracts.PaymentTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentTypeManagerTest {

    @Mock
    private PaymentTypeRepository paymentTypeRepository;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private PaymentTypeManager paymentTypeManager;

    @Test
    void getByName_whenTypeExists_shouldReturnType() {
        PaymentTypeEnum typeEnum = PaymentTypeEnum.CREDIT_CARD;
        PaymentType type = new PaymentType();
        type.setTypeName(typeEnum);

        when(paymentTypeRepository.findByTypeName(typeEnum)).thenReturn(Optional.of(type));

        PaymentType result = paymentTypeManager.getByName(typeEnum);

        assertNotNull(result);
        assertEquals(typeEnum, result.getTypeName());
        verify(paymentTypeRepository).findByTypeName(typeEnum);
    }

    @Test
    void getByName_whenTypeMissing_shouldThrowNotFoundException() {
        PaymentTypeEnum typeEnum = PaymentTypeEnum.CREDIT_CARD;
        when(paymentTypeRepository.findByTypeName(typeEnum)).thenReturn(Optional.empty());
        // FIX: Changed getMessage to getMessageWithParams
        when(messageService.getMessageWithParams(any(), any())).thenReturn("Error");

        assertThrows(NotFoundException.class, () -> paymentTypeManager.getByName(typeEnum));
    }
}
