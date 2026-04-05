package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.entities.concretes.Carrier;
import com.project.ecommerce_backend.repositories.abstracts.CarrierRepository;
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
class CarrierManagerTest {

    @Mock
    private CarrierRepository carrierRepository;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private CarrierManager carrierManager;

    @Test
    void getByIdAsEntity_whenCarrierExists_shouldReturnCarrier() {
        // Arrange
        Long id = 1L;
        Carrier carrier = new Carrier();
        carrier.setId(id);

        when(carrierRepository.findById(id)).thenReturn(Optional.of(carrier));

        // Act
        Carrier result = carrierManager.getByIdAsEntity(id);

        // Assert
        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(carrierRepository).findById(id);
    }

    @Test
    void getByIdAsEntity_whenCarrierDoesNotExist_shouldThrowNotFoundException() {
        // Arrange
        Long id = 1L;
        when(carrierRepository.findById(id)).thenReturn(Optional.empty());
        when(messageService.getMessageWithParams(any(), any())).thenReturn("Error");

        // Act & Assert
        assertThrows(NotFoundException.class, () -> carrierManager.getByIdAsEntity(id));
        verify(carrierRepository).findById(id);
    }
}
