package com.project.ecommerce_backend.repositories.abstracts;

import com.project.ecommerce_backend.entities.concretes.Invoice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("SELECT i FROM Invoice i WHERE i.order.customer.id = :customerId")
    Slice<Invoice> findSliceByCustomerId(@Param("customerId") Long customerId, Pageable pageable);

    @Query("SELECT i FROM Invoice i " +
            "LEFT JOIN FETCH i.order o " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.productItem pi " +
            "LEFT JOIN FETCH pi.product " +
            "LEFT JOIN FETCH pi.colour " +
            "LEFT JOIN FETCH pi.size " +
            "LEFT JOIN FETCH i.paymentMethod " +
            "WHERE i.id = :invoiceId AND i.order.customer.id = :customerId")
    Optional<Invoice> findByIdAndCustomerIdWithDetails(@Param("invoiceId") Long invoiceId, @Param("customerId") Long customerId);

    @Query("SELECT i.order.customer.id FROM Invoice i WHERE i.id = :invoiceId")
    Long findCustomerIdByInvoiceId(@Param("invoiceId") Long invoiceId);
}