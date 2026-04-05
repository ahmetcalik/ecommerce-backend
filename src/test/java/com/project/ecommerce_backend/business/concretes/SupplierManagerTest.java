package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.helpers.CacheHelper;
import com.project.ecommerce_backend.core.exceptions.types.BusinessException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.entities.concretes.Supplier;
import com.project.ecommerce_backend.repositories.abstracts.SupplierRepository;
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
class SupplierManagerTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private MessageService messageService;

    @Mock
    private CacheHelper cacheHelper;

    @InjectMocks
    private SupplierManager supplierManager;

    // 1. getByIdAsEntity TESTS
    @Test
    void getByIdAsEntity_whenSupplierExists_shouldReturnSupplier() {
        Long id = 1L;
        Supplier supplier = new Supplier();
        supplier.setId(id);

        when(supplierRepository.findById(id)).thenReturn(Optional.of(supplier));

        Supplier result = supplierManager.getByIdAsEntity(id);

        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(supplierRepository).findById(id);
    }

    @Test
    void getByIdAsEntity_whenSupplierMissing_shouldThrowRuntimeException() {
        Long id = 1L;
        when(supplierRepository.findById(id)).thenReturn(Optional.empty());
        when(messageService.getMessageWithParams(any(), any())).thenReturn("Error");

        assertThrows(RuntimeException.class, () -> supplierManager.getByIdAsEntity(id));
    }

    // 2. getAuthenticatedSupplierAsEntity TESTS
    @Test
    void getAuthenticatedSupplierAsEntity_whenActive_shouldReturnSupplier() {
        Long userId = 10L;
        Supplier supplier = new Supplier();
        supplier.setIsActive(true);

        when(cacheHelper.getAuthenticatedUserId()).thenReturn(userId);
        when(supplierRepository.findByCUser(userId)).thenReturn(Optional.of(supplier));

        Supplier result = supplierManager.getAuthenticatedSupplierAsEntity();

        assertNotNull(result);
        assertTrue(result.getIsActive());
    }

    @Test
    void getAuthenticatedSupplierAsEntity_whenMissing_shouldThrowBusinessException() {
        Long userId = 10L;
        when(cacheHelper.getAuthenticatedUserId()).thenReturn(userId);
        when(supplierRepository.findByCUser(userId)).thenReturn(Optional.empty());
        when(messageService.getMessage(any())).thenReturn("Error");

        assertThrows(BusinessException.class, () -> supplierManager.getAuthenticatedSupplierAsEntity());
    }

    @Test
    void getAuthenticatedSupplierAsEntity_whenInactive_shouldThrowBusinessException() {
        Long userId = 10L;
        Supplier supplier = new Supplier();
        supplier.setIsActive(false);

        when(cacheHelper.getAuthenticatedUserId()).thenReturn(userId);
        when(supplierRepository.findByCUser(userId)).thenReturn(Optional.of(supplier));
        when(messageService.getMessage(any())).thenReturn("Error");

        assertThrows(BusinessException.class, () -> supplierManager.getAuthenticatedSupplierAsEntity());
    }
}