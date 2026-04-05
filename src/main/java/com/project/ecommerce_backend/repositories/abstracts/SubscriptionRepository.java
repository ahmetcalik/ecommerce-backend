package com.project.ecommerce_backend.repositories.abstracts;

import com.project.ecommerce_backend.entities.concretes.Subscription;
import com.project.ecommerce_backend.entities.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByPaymentMethodIdAndStatus(Long paymentMethodId, SubscriptionStatus status);
}