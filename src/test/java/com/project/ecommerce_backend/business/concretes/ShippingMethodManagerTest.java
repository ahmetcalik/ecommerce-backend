package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.entities.concretes.ShippingMethod;
import com.project.ecommerce_backend.repositories.abstracts.ShippingMethodRepository;
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
class ShippingMethodManagerTest {

    @Mock
    private ShippingMethodRepository shippingMethodRepository;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private ShippingMethodManager shippingMethodManager;

    @Test
    void getByIdAsEntity_whenMethodExists_shouldReturnMethod() {
        Long id = 1L;
        ShippingMethod method = new ShippingMethod();
        method.setId(id);

        when(shippingMethodRepository.findById(id)).thenReturn(Optional.of(method));

        ShippingMethod result = shippingMethodManager.getByIdAsEntity(id);

        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(shippingMethodRepository).findById(id);
    }

    @Test
    void getByIdAsEntity_whenMethodMissing_shouldThrowNotFoundException() {
        Long id = 1L;
        when(shippingMethodRepository.findById(id)).thenReturn(Optional.empty());
        when(messageService.getMessageWithParams(any(), any())).thenReturn("Error");

        assertThrows(NotFoundException.class, () -> shippingMethodManager.getByIdAsEntity(id));
    }
}
