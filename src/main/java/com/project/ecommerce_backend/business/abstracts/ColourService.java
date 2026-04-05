package com.project.ecommerce_backend.business.abstracts;

import com.project.ecommerce_backend.entities.concretes.Colour;

public interface ColourService {

    Colour getByIdAsEntity(Long id);

}