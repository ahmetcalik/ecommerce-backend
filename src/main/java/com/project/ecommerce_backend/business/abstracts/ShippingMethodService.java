package com.project.ecommerce_backend.business.abstracts;

import com.project.ecommerce_backend.entities.concretes.ShippingMethod;

public interface ShippingMethodService {

    ShippingMethod getByIdAsEntity(Long id);

}