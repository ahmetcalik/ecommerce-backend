package com.project.ecommerce_backend.business.abstracts;

public interface SubscriptionService {

    boolean isPaymentMethodInUseByActiveSubscription(Long paymentMethodId);

}