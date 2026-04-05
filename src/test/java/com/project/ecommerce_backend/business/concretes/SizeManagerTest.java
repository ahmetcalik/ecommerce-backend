package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.entities.concretes.Size;
import com.project.ecommerce_backend.repositories.abstracts.SizeRepository;
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
class SizeManagerTest {

    @Mock
    private SizeRepository sizeRepository;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private SizeManager sizeManager;

    @Test
    void getByIdAsEntity_whenSizeExists_shouldReturnSize() {
        Long id = 1L;
        Size size = new Size();
        size.setId(id);

        when(sizeRepository.findById(id)).thenReturn(Optional.of(size));

        Size result = sizeManager.getByIdAsEntity(id);

        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(sizeRepository).findById(id);
    }

    @Test
    void getByIdAsEntity_whenSizeMissing_shouldThrowNotFoundException() {
        Long id = 1L;
        when(sizeRepository.findById(id)).thenReturn(Optional.empty());
        // FIX: Changed getMessageWithParams to getMessage
        when(messageService.getMessage(any())).thenReturn("Error");

        assertThrows(NotFoundException.class, () -> sizeManager.getByIdAsEntity(id));
    }
}
