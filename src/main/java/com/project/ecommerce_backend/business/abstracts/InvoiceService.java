package com.project.ecommerce_backend.business.abstracts;

import com.project.ecommerce_backend.business.dtos.responses.invoice.ListUserInvoicesResponse;
import com.project.ecommerce_backend.business.dtos.responses.invoice.UserInvoiceDetailResponse;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.core.utils.result.Result;
import com.project.ecommerce_backend.entities.concretes.Invoice;
import com.project.ecommerce_backend.entities.concretes.Order;
import com.project.ecommerce_backend.entities.concretes.PaymentMethod;
import com.project.ecommerce_backend.entities.concretes.ShippingMethod;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface InvoiceService {

    Invoice createInvoiceForOrder(Order order, PaymentMethod paymentMethod,
                                  BigDecimal productSubTotal, BigDecimal productVatAmount,
                                  BigDecimal interestAmount, Integer installmentCount,
                                  ShippingMethod shippingMethod);

    DataResult<List<ListUserInvoicesResponse>> getAllMyInvoices(Pageable pageable);

    DataResult<UserInvoiceDetailResponse> getMyInvoiceById(Long id);

    Result cancelInvoice(Long invoiceId);

    Result markInvoiceAsRefunded(Long invoiceId);

}