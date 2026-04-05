package com.project.ecommerce_backend.repositories.abstracts;

import com.project.ecommerce_backend.entities.concretes.CustomerAddress;
import com.project.ecommerce_backend.entities.concretes.CustomerAddressId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, CustomerAddressId> {

    /**
     * Belirli bir adresin belirli bir müşteriye ait olup olmadığını
     * doğrulamak için kullanılan bağlantı kaydını bulur.
     */
    Optional<CustomerAddress> findByAddressIdAndCustomerId(Long addressId, Long customerId);

    List<CustomerAddress> findByCustomerIdAndIsShippingAddressTrue(Long customerId);

    List<CustomerAddress> findByCustomerIdAndIsBillingAddressTrue(Long customerId);

    @Query("SELECT ca FROM CustomerAddress ca " +
            "WHERE ca.customer.id = :customerId AND ca.address.id <> :addressId " +
            "ORDER BY ca.cDate DESC")
    List<CustomerAddress> findPotentialNewDefaultAddresses(
            @Param("customerId") Long customerId,
            @Param("addressId") Long addressId);

    long countByAddressId(Long addressId);

    boolean existsByCustomerIdAndAddressId(Long customerId, Long addressId);

}