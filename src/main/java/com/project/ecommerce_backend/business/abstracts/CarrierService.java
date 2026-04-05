package com.project.ecommerce_backend.business.abstracts;

import com.project.ecommerce_backend.entities.concretes.Carrier;

public interface CarrierService {

    Carrier getByIdAsEntity(Long id);

}