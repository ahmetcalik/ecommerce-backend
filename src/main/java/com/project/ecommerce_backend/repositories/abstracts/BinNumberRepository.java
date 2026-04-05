package com.project.ecommerce_backend.repositories.abstracts;

import com.project.ecommerce_backend.entities.concretes.BinNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BinNumberRepository extends JpaRepository<BinNumber, String> {

    @Query("SELECT b FROM BinNumber b WHERE ?1 LIKE CONCAT(b.id, '%') ORDER BY LENGTH(b.id) DESC LIMIT 1")
    Optional<BinNumber> findLongestMatchingBin(String cardNumberPrefix);
}