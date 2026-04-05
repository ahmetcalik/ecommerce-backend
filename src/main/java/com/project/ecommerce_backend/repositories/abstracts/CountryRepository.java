package com.project.ecommerce_backend.repositories.abstracts;

import com.project.ecommerce_backend.entities.concretes.Country;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, Long> {
}