package com.project.ecommerce_backend.repositories.abstracts;

import com.project.ecommerce_backend.business.dtos.responses.address.ListAddressResponse;
import com.project.ecommerce_backend.entities.concretes.Address;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    Optional<Address> findByAddressLine1AndAddressLine2AndCityAndPostalCodeAndCountryId(
            String addressLine1, String addressLine2, String city, String postalCode, Long countryId);

    @Query("SELECT NEW com.project.ecommerce_backend.business.dtos.responses.address.ListAddressResponse(" +
            "a.id, a.title, a.addressLine1, a.city, c.countryName, ca.isShippingAddress, ca.isBillingAddress) " +
            "FROM Address a " +
            "JOIN a.country c " +
            "JOIN a.customerAddresses ca " +
            "WHERE ca.customer.id = :customerId")
    Slice<ListAddressResponse> findSliceByCustomerIdAsDto(@Param("customerId") Long customerId, Pageable pageable);
}