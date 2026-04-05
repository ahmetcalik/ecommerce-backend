package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.dtos.responses.country.CountryResponse;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.utils.mapper.ModelMapperService;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.entities.concretes.Country;
import com.project.ecommerce_backend.repositories.abstracts.CountryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CountryManagerTest {

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private ModelMapperService modelMapperService;

    @Mock
    private MessageService messageService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private CountryManager countryManager;

    @BeforeEach
    void setUp() {
        lenient().when(modelMapperService.getMapper()).thenReturn(modelMapper);
    }

    @Test
    void getAll_ShouldReturnCountryList_WhenCountriesExist() {
        // Arrange
        Country country = new Country();
        country.setId(1L);
        country.setCountryName("Turkey");

        CountryResponse countryResponse = new CountryResponse();
        countryResponse.setId(1L);
        countryResponse.setCountryName("Turkey");

        when(countryRepository.findAll()).thenReturn(List.of(country));
        when(modelMapper.map(country, CountryResponse.class)).thenReturn(countryResponse);
        when(messageService.getMessage(Messages.Country.COUNTRY_SUCCESSFULLY_LISTED))
                .thenReturn("Countries listed successfully");

        // Act
        DataResult<List<CountryResponse>> result = countryManager.getAll();

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals("Turkey", result.getData().get(0).getCountryName());
        assertEquals("Countries listed successfully", result.getMessage());

        verify(countryRepository).findAll();
        verify(modelMapper).map(country, CountryResponse.class);
        verify(messageService).getMessage(Messages.Country.COUNTRY_SUCCESSFULLY_LISTED);
    }

    @Test
    void getAll_ShouldReturnEmptyList_WhenNoCountriesExist() {
        // Arrange
        when(countryRepository.findAll()).thenReturn(Collections.emptyList());
        when(messageService.getMessage(Messages.Country.COUNTRY_SUCCESSFULLY_LISTED))
                .thenReturn("Countries listed successfully");

        // Act
        DataResult<List<CountryResponse>> result = countryManager.getAll();

        // Assert
        assertTrue(result.isSuccess());
        assertTrue(result.getData().isEmpty());
        assertEquals("Countries listed successfully", result.getMessage());

        verify(countryRepository).findAll();
    }

    @Test
    void getByIdAsEntity_ShouldReturnCountry_WhenIdExists() {
        // Arrange
        Long id = 1L;
        Country country = new Country();
        country.setId(id);
        country.setCountryName("Turkey");

        when(countryRepository.findById(id)).thenReturn(Optional.of(country));

        // Act
        Country result = countryManager.getByIdAsEntity(id);

        // Assert
        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals("Turkey", result.getCountryName());

        verify(countryRepository).findById(id);
    }

    @Test
    void getByIdAsEntity_ShouldThrowNotFoundException_WhenIdDoesNotExist() {
        // Arrange
        Long id = 1L;
        when(countryRepository.findById(id)).thenReturn(Optional.empty());
        when(messageService.getMessageWithParams(Messages.Country.COUNTRY_DOES_NOT_EXISTS_WITH_GIVEN_ID, id))
                .thenReturn("Country not found");

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> countryManager.getByIdAsEntity(id));
        assertEquals("Country not found", exception.getMessage());

        verify(countryRepository).findById(id);
    }
}