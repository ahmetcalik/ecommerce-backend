package com.project.ecommerce_backend.business.abstracts;

import com.project.ecommerce_backend.business.dtos.requests.address.AddressRequest;
import com.project.ecommerce_backend.business.dtos.responses.address.AddressDetailResponse;
import com.project.ecommerce_backend.business.dtos.responses.address.ListAddressResponse;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.core.utils.result.Result;
import com.project.ecommerce_backend.entities.concretes.Address;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AddressService {

    DataResult<AddressDetailResponse> getById(Long id);

    DataResult<List<ListAddressResponse>> getAll(Pageable pageable);

    DataResult<AddressDetailResponse> add(AddressRequest request);

    DataResult<AddressDetailResponse> update(Long addressId, AddressRequest request);

    Result delete(Long addressId);

    Address getByIdAsEntity(Long id);

    Address getByIdAndCustomerId(Long id, Long customerId);

}