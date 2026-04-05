package com.project.ecommerce_backend.repositories.abstracts;

import com.project.ecommerce_backend.entities.concretes.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findAllByCustomerId(Long customerId);

    Optional<PaymentMethod> findByIdAndCustomerId(Long id, Long customerId);

    boolean existsByCustomerIdAndLastFourDigitsAndExpiryMonthAndExpiryYear(
            Long customerId, String lastFourDigits, Integer expiryMonth, Integer expiryYear);
}