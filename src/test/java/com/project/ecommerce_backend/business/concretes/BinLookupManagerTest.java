package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.entities.concretes.BinNumber;
import com.project.ecommerce_backend.repositories.abstracts.BinNumberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BinLookupManagerTest {

    @Mock
    private BinNumberRepository binNumberRepository;

    @InjectMocks
    private BinLookupManager binLookupManager;

    @Test
    void getCardFamilyByBinNumber_whenBinExists_shouldReturnFamilyName() {
        String binNumber = "123456";
        String familyName = "Bonus";
        BinNumber bin = new BinNumber();
        bin.setCardFamilyName(familyName);

        when(binNumberRepository.findLongestMatchingBin(binNumber)).thenReturn(Optional.of(bin));

        String result = binLookupManager.getCardFamilyByBinNumber(binNumber);

        assertEquals(familyName, result);
        verify(binNumberRepository).findLongestMatchingBin(binNumber);
    }

    @Test
    void getCardFamilyByBinNumber_whenBinMissing_shouldReturnDefault() {
        String binNumber = "999999";

        when(binNumberRepository.findLongestMatchingBin(binNumber)).thenReturn(Optional.empty());

        String result = binLookupManager.getCardFamilyByBinNumber(binNumber);

        assertEquals("DEFAULT", result);
        verify(binNumberRepository).findLongestMatchingBin(binNumber);
    }
}
