package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.entities.concretes.Colour;
import com.project.ecommerce_backend.repositories.abstracts.ColourRepository;
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
class ColourManagerTest {

    @Mock
    private ColourRepository colourRepository;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private ColourManager colourManager;

    @Test
    void getByIdAsEntity_whenColourExists_shouldReturnColour() {
        Long id = 1L;
        Colour colour = new Colour();
        colour.setId(id);

        when(colourRepository.findById(id)).thenReturn(Optional.of(colour));

        Colour result = colourManager.getByIdAsEntity(id);

        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(colourRepository).findById(id);
    }

    @Test
    void getByIdAsEntity_whenColourMissing_shouldThrowNotFoundException() {
        Long id = 1L;
        when(colourRepository.findById(id)).thenReturn(Optional.empty());
        when(messageService.getMessageWithParams(any(), any())).thenReturn("Error");

        assertThrows(NotFoundException.class, () -> colourManager.getByIdAsEntity(id));
    }
}
