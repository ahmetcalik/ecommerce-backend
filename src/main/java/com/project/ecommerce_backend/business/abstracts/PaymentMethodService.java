package com.project.ecommerce_backend.business.abstracts;

import com.project.ecommerce_backend.business.dtos.requests.payment.AddPaymentMethodRequest;
import com.project.ecommerce_backend.business.dtos.requests.payment.UpdatePaymentMethodRequest;
import com.project.ecommerce_backend.business.dtos.responses.payment.ListPaymentMethodResponse;
import com.project.ecommerce_backend.business.dtos.responses.payment.PaymentMethodDetailResponse;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.core.utils.result.Result;
import com.project.ecommerce_backend.entities.concretes.PaymentMethod;

import java.util.List;

public interface PaymentMethodService {

    DataResult<List<ListPaymentMethodResponse>> getAll();

    DataResult<PaymentMethodDetailResponse> getById(Long id);

    DataResult<PaymentMethodDetailResponse> add(AddPaymentMethodRequest request);

    DataResult<PaymentMethodDetailResponse> update(Long paymentMethodId, UpdatePaymentMethodRequest request);

    Result delete(Long paymentMethodId);

    PaymentMethod getByIdAndCustomerId(Long id, Long customerId);

}