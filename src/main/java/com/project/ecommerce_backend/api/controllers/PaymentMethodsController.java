package com.project.ecommerce_backend.api.controllers;

import com.project.ecommerce_backend.business.abstracts.PaymentMethodService;
import com.project.ecommerce_backend.business.dtos.requests.payment.AddPaymentMethodRequest;
import com.project.ecommerce_backend.business.dtos.requests.payment.UpdatePaymentMethodRequest;
import com.project.ecommerce_backend.business.dtos.responses.payment.ListPaymentMethodResponse;
import com.project.ecommerce_backend.business.dtos.responses.payment.PaymentMethodDetailResponse;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.core.utils.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bu controller, sistemde desteklenen tüm ödeme kanallarının yönetim merkezidir.
 * Kullanıcılarımızın ödeme adımında hangi yöntemleri kullanabileceğini buradan sunduğumuz veriler belirler.
 * Güvenlik nedeniyle, yeni bir ödeme yöntemi tanımlama veya mevcut olanları pasife çekme gibi işlemler sadece 'ADMIN' rolündeki yöneticiler tarafından gerçekleştirilebilir.
 */
@RestController
@RequestMapping("/api/v1/payment-methods")
@RequiredArgsConstructor
@Validated
public class PaymentMethodsController {

    private final PaymentMethodService paymentMethodService;

    /**
     * Sistemde aktif olarak sunulan tüm ödeme yöntemlerini listeleriz.
     * Bu endpoint, ödeme sayfasında müşteriye sunulacak seçenekleri doldurmak için kullanılır.
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public DataResult<List<ListPaymentMethodResponse>> getAll() {
        return paymentMethodService.getAll();
    }

    /**
     * Belirli bir ödeme yönteminin teknik detaylarına veya yapılandırma bilgilerine ulaşmak için kullanılır.
     */
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public DataResult<PaymentMethodDetailResponse> getById(
            @PathVariable @Min(value = 1, message = Messages.Validations.Common.INVALID_ID) Long id) {
        return paymentMethodService.getById(id);
    }

    /**
     * Sisteme yeni bir ödeme kanalı entegre edildiğinde bu metodu kullanıyoruz.
     * Örneğin, yeni bir banka sanal posu veya dijital cüzdan seçeneği eklemek için yönetici tarafından tetiklenir.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public DataResult<PaymentMethodDetailResponse> add(
            @Valid @RequestBody AddPaymentMethodRequest request) {
        return paymentMethodService.add(request);
    }

    /**
     * Mevcut bir ödeme yönteminin adını, aktiflik durumunu veya öncelik sırasını güncellememizi sağlar.
     */
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public DataResult<PaymentMethodDetailResponse> update(
            @PathVariable @Min(value = 1, message = Messages.Validations.Common.INVALID_ID) Long id,
            @Valid @RequestBody UpdatePaymentMethodRequest request) {
        return paymentMethodService.update(id, request);
    }

    /**
     * Artık kullanılmayan veya geçici olarak devre dışı bırakılması gereken bir ödeme yöntemini sistemden kaldırır.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public Result delete(
            @PathVariable @Min(value = 1, message = Messages.Validations.Common.INVALID_ID) Long id) {
        return paymentMethodService.delete(id);
    }
}