package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.SubscriptionService;
import com.project.ecommerce_backend.entities.enums.SubscriptionStatus;
import com.project.ecommerce_backend.repositories.abstracts.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Kullanıcıların abonelik süreçlerini denetleyen ve finansal taahhütlerin yönetiminden sorumlu olan servis katmanıdır.
 * Bu sınıf; özellikle aktif aboneliklerin bağlı olduğu ödeme yöntemlerini analiz ederek, sistemin tahsilat süreçlerinde herhangi bir kesinti yaşanmamasını garanti altına alır. Diğer finansal servislerle entegre çalışarak, aktif bir ödeme planına bağlı olan verilerin korunması ve sistem tutarlılığının sağlanması görevlerini üstlenir.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionManager implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    /**
     * Belirtilen ödeme yönteminin halihazırda devam eden ve aktif statüdeki bir abonelik tarafından kullanılıp kullanılmadığını denetler.
     * Bu kontrol; kullanıcıların ödeme yöntemlerini sistemden kaldırma taleplerinde bir emniyet mekanizması görevi görerek, aktif bir aboneliğin tahsilat kaynağının fiziksel olarak silinmesini engeller ve finansal operasyonun sürekliliğini korur.
     *
     * @param paymentMethodId Kullanım durumu kontrol edilecek olan ödeme yönteminin benzersiz kimlik numarasıdır.
     * @return Ödeme yöntemi aktif bir aboneliğe bağlıysa true değerini döner.
     */
    @Override
    public boolean isPaymentMethodInUseByActiveSubscription(Long paymentMethodId) {
        return this.subscriptionRepository.existsByPaymentMethodIdAndStatus(
                paymentMethodId, SubscriptionStatus.ACTIVE
        );
    }
}