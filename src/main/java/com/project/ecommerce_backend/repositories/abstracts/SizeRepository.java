package com.project.ecommerce_backend.repositories.abstracts;

import com.project.ecommerce_backend.entities.concretes.Size;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SizeRepository extends JpaRepository<Size, Long> {
}
