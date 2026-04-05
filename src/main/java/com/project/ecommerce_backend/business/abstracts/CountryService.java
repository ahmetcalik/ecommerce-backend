package com.project.ecommerce_backend.business.abstracts;

import com.project.ecommerce_backend.business.dtos.responses.country.CountryResponse;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.entities.concretes.Country;
import java.util.List;

public interface CountryService {

    DataResult<List<CountryResponse>> getAll();

    Country getByIdAsEntity(Long id);

}