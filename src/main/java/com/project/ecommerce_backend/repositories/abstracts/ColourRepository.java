package com.project.ecommerce_backend.repositories.abstracts;

import com.project.ecommerce_backend.entities.concretes.Colour;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColourRepository extends JpaRepository<Colour, Long> {
}
