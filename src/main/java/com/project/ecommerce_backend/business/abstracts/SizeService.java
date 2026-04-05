package com.project.ecommerce_backend.business.abstracts;

import com.project.ecommerce_backend.entities.concretes.Size;

public interface SizeService {

    Size getByIdAsEntity(Long id);

}