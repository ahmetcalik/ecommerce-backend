package com.project.ecommerce_backend.repositories.abstracts;

import com.project.ecommerce_backend.entities.concretes.InstallmentOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InstallmentOptionRepository extends JpaRepository<InstallmentOption, Long> {

    List<InstallmentOption> findByCardFamilyNameInAndIsActiveTrue(List<String> cardFamilyNames);

    Optional<InstallmentOption> findByCardFamilyNameInAndNumberOfInstallmentsAndIsActiveTrue(
            List<String> cardFamilyNames, Integer numberOfInstallments);

}