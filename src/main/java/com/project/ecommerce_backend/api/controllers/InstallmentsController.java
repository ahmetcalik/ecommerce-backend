package com.project.ecommerce_backend.api.controllers;

import com.project.ecommerce_backend.business.abstracts.BinLookupService;
import com.project.ecommerce_backend.business.abstracts.InstallmentService;
import com.project.ecommerce_backend.business.dtos.requests.installment.CalculateInstallmentRequest;
import com.project.ecommerce_backend.business.dtos.responses.installment.InstallmentOptionResponse;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.core.utils.result.SuccessDataResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bu controller, ödeme sürecinin en önemli parçalarından biri olan taksit seçeneklerini yönetir.
 * Kullanıcının girdiği kredi kartı bilgilerine (BIN numarasına) dayanarak, sepet tutarına uygun hangi taksit imkanlarının sunulabileceğini dinamik olarak hesaplıyoruz.
 * Hem müşteri memnuniyeti hem de ödeme sistemlerinin entegrasyonu için bu hesaplama motoru kilit bir rol oynamaktadır.
 */
@RestController
@RequestMapping("/api/v1/installments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'SELLER', 'ADMIN')")
@Validated
public class InstallmentsController {

    private final InstallmentService installmentService;
    private final BinLookupService binLookupService;
    private final MessageService messageService;

    /**
     * Girilen kart bilgilerine göre kullanılabilir taksit seçeneklerini döndürür.
     * Önce kartın hangi banka ailesine ait olduğunu sorgular, ardından o tutar için tanımlanmış güncel taksit oranlarını getirir.
     * Ödeme sayfasında kart numarası girilir girilmez taksit tablosunu güncellemek için kullanılır.
     */
    @PostMapping("/options")
    public DataResult<List<InstallmentOptionResponse>> getInstallmentOptions(
            @Valid @RequestBody CalculateInstallmentRequest request) {

        String cardFamily = binLookupService.getCardFamilyByBinNumber(request.getBinNumber());

        List<InstallmentOptionResponse> options = installmentService.getAvailableOptions(
                request.getAmount(), cardFamily);

        return new SuccessDataResult<>(options, messageService.getMessage(
                Messages.Installment.INSTALLMENT_OPTIONS_SUCCESSFULLY_LISTED));
    }
}