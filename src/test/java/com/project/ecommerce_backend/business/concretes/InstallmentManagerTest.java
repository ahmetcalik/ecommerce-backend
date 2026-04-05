package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.core.exceptions.types.BusinessException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.entities.concretes.InstallmentOption;
import com.project.ecommerce_backend.repositories.abstracts.InstallmentOptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstallmentManagerTest {

    @Mock
    private InstallmentOptionRepository installmentOptionRepository;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private InstallmentManager installmentManager;

    // 1. getAvailableOptions TESTS
    @Test
    void getAvailableOptions_shouldReturnCalculatedOptions() {
        BigDecimal baseAmount = BigDecimal.valueOf(100);
        String cardFamily = "Visa";

        InstallmentOption option = new InstallmentOption();
        option.setNumberOfInstallments(2);
        option.setInterestRate(BigDecimal.valueOf(0.10)); // %10 faiz

        when(installmentOptionRepository.findByCardFamilyNameInAndIsActiveTrue(any())).thenReturn(List.of(option));

        var result = installmentManager.getAvailableOptions(baseAmount, cardFamily);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getInstallmentCount());
        // 100 * 1.10 = 110 toplam
        assertEquals(BigDecimal.valueOf(110.00).setScale(2), result.get(0).getTotalAmount());
    }

    // 2. getSpecificOption TESTS
    @Test
    void getSpecificOption_whenValid_shouldReturnOption() {
        BigDecimal baseAmount = BigDecimal.valueOf(100);
        String cardFamily = "Visa";
        int installmentCount = 2;

        InstallmentOption option = new InstallmentOption();
        option.setNumberOfInstallments(installmentCount);
        option.setInterestRate(BigDecimal.valueOf(0.10));

        when(installmentOptionRepository.findByCardFamilyNameInAndNumberOfInstallmentsAndIsActiveTrue(any(), anyInt()))
                .thenReturn(Optional.of(option));

        var result = installmentManager.getSpecificOption(baseAmount, cardFamily, installmentCount);

        assertNotNull(result);
        assertEquals(installmentCount, result.getInstallmentCount());
        assertEquals(BigDecimal.valueOf(110.00).setScale(2), result.getTotalAmount());
    }

    @Test
    void getSpecificOption_whenInvalid_shouldThrowBusinessException() {
        BigDecimal baseAmount = BigDecimal.valueOf(100);
        String cardFamily = "Visa";
        int installmentCount = 99;

        when(installmentOptionRepository.findByCardFamilyNameInAndNumberOfInstallmentsAndIsActiveTrue(any(), anyInt()))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(any())).thenReturn("Error");

        assertThrows(BusinessException.class, () -> installmentManager.getSpecificOption(baseAmount, cardFamily, installmentCount));
    }
}
