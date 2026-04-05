package com.project.ecommerce_backend.business.abstracts;

import com.project.ecommerce_backend.business.dtos.requests.auth.RegisterRequest;
import com.project.ecommerce_backend.business.dtos.responses.auth.AuthenticationResponse;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.core.utils.result.Result;
import com.project.ecommerce_backend.entities.concretes.Customer;

public interface CustomerService {

    DataResult<AuthenticationResponse> register(RegisterRequest request);

    Result grantRoleToUser(Long customerId, String roleName);

    Result revokeRoleFromUser(Long customerId, String roleName);

    Result deleteUser(Long customerId);

    Customer getByIdAsEntity(Long id);

}
