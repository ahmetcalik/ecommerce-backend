package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.entities.enums.SubscriptionStatus;
import com.project.ecommerce_backend.repositories.abstracts.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionManagerTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionManager subscriptionManager;

    @Test
    void isPaymentMethodInUseByActiveSubscription_whenInUse_shouldReturnTrue() {
        Long paymentMethodId = 1L;
        when(subscriptionRepository.existsByPaymentMethodIdAndStatus(paymentMethodId, SubscriptionStatus.ACTIVE))
                .thenReturn(true);

        boolean result = subscriptionManager.isPaymentMethodInUseByActiveSubscription(paymentMethodId);

        assertTrue(result);
        verify(subscriptionRepository).existsByPaymentMethodIdAndStatus(paymentMethodId, SubscriptionStatus.ACTIVE);
    }

    @Test
    void isPaymentMethodInUseByActiveSubscription_whenNotInUse_shouldReturnFalse() {
        Long paymentMethodId = 1L;
        when(subscriptionRepository.existsByPaymentMethodIdAndStatus(paymentMethodId, SubscriptionStatus.ACTIVE))
                .thenReturn(false);

        boolean result = subscriptionManager.isPaymentMethodInUseByActiveSubscription(paymentMethodId);

        assertFalse(result);
        verify(subscriptionRepository).existsByPaymentMethodIdAndStatus(paymentMethodId, SubscriptionStatus.ACTIVE);
    }
}
