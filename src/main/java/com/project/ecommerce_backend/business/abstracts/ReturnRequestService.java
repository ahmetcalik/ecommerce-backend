package com.project.ecommerce_backend.business.abstracts;

import com.project.ecommerce_backend.business.dtos.requests.return_request.AddReturnRequest;
import com.project.ecommerce_backend.business.dtos.requests.return_request.UpdateReturnStatusRequest;
import com.project.ecommerce_backend.business.dtos.responses.return_request.ReturnRequestResponse;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface ReturnRequestService {

    DataResult<ReturnRequestResponse> add(AddReturnRequest request);

    DataResult<Slice<ReturnRequestResponse>> getAllReturns(Pageable pageable);

    DataResult<ReturnRequestResponse> getReturnByIdForAdmin(Long id);

    DataResult<ReturnRequestResponse> updateReturnStatus(Long id, UpdateReturnStatusRequest request);

}