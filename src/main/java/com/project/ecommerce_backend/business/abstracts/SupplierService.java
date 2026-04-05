package com.project.ecommerce_backend.business.abstracts;

import com.project.ecommerce_backend.entities.concretes.Supplier;

public interface SupplierService {

    Supplier getByIdAsEntity(Long id);

    Supplier getAuthenticatedSupplierAsEntity();

}