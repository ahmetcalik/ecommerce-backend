package com.project.ecommerce_backend.api.controllers;

import com.project.ecommerce_backend.business.abstracts.ReturnRequestService;
import com.project.ecommerce_backend.business.dtos.requests.return_request.AddReturnRequest;
import com.project.ecommerce_backend.business.dtos.responses.return_request.ReturnRequestResponse;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Bu controller, müşterilerimizin satın aldıkları ürünler için iade sürecini başlattıkları merkezdir.
 * Satış sonrası destek sürecinin ilk adımı burada atılır.
 * Kullanıcıların yasal iade haklarını kullanabilmeleri için gereken talepleri sistemli bir şekilde kayıt altına alıyoruz.
 * Güvenlik gereği bu sınıftaki işlemler sadece 'USER' rolüne sahip müşterilerimize açıktır; iade talebinin doğruluğu ve sipariş sahipliği servis katmanında titizlikle denetlenir.
 */
@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@Validated
public class ReturnRequestsController {

    private final ReturnRequestService returnRequestService;

    /**
     * Müşterinin belirli bir siparişteki ürün veya ürünler için iade isteği oluşturmasını sağlayan endpoint.
     * Bu işlem tetiklendiğinde iade nedeni ve açıklamalar sisteme kaydedilir, ardından operasyon ekibinin inceleme süreci başlar.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DataResult<ReturnRequestResponse> add(
            @Valid @RequestBody AddReturnRequest request) {
        return returnRequestService.add(request);
    }
}