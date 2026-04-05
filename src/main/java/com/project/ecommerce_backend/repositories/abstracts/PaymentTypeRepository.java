package com.project.ecommerce_backend.repositories.abstracts;

import com.project.ecommerce_backend.entities.concretes.PaymentType;
import com.project.ecommerce_backend.entities.enums.PaymentTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentTypeRepository extends JpaRepository<PaymentType, Long> {
    Optional<PaymentType> findByTypeName(PaymentTypeEnum typeName);
}