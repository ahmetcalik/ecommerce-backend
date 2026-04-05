package com.project.ecommerce_backend.repositories.abstracts;

import com.project.ecommerce_backend.entities.concretes.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    boolean existsByOrderItemId(Long orderItemId);

    boolean existsByOrderIdAndOrderItemId(Long orderId, Long orderItemId);

    @Query("SELECT CASE WHEN COUNT(rr) > 0 THEN TRUE ELSE FALSE END " +
            "FROM ReturnRequest rr JOIN rr.orderItem oi JOIN oi.productItem pi JOIN pi.product p " +
            "WHERE rr.id = :returnRequestId AND p.supplier.cUser = :supplierId")
    boolean existsByIdAndOrderItemProductItemProductSupplierId(
            @Param("returnRequestId") Long returnRequestId,
            @Param("supplierId") Long supplierId);

}