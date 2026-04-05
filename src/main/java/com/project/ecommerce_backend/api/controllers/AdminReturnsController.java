package com.project.ecommerce_backend.api.controllers;

import com.project.ecommerce_backend.business.abstracts.ReturnRequestService;
import com.project.ecommerce_backend.business.dtos.requests.return_request.UpdateReturnStatusRequest;
import com.project.ecommerce_backend.business.dtos.responses.return_request.ReturnRequestResponse;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Bu controller, müşterilerden gelen iade taleplerinin yönetim panelindeki karşılığıdır.
 * Satıcılar ve sistem yöneticileri, ürünlerin geri gönderilme nedenlerini inceleyebilir ve iade sürecini onaylama, reddetme veya kargo takibi gibi aşamalarla buradan yönetirler.
 * Finansal iadelerin sağlıklı yapılabilmesi için buradaki statü güncellemeleri kritik önem taşır.
 */
@RestController
@RequestMapping("/api/v1/admin/returns")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
@Validated
public class AdminReturnsController {

    private final ReturnRequestService returnRequestService;

    /**
     * Sistemdeki tüm iade taleplerini bir liste halinde operasyon ekibine sunarız.
     * Sayfalama yapısı sayesinde yoğun iade dönemlerinde bile performanstan ödün vermeden
     * tüm taleplerin üzerinden geçilebilmesini sağlıyoruz.
     */
    @GetMapping
    public DataResult<Slice<ReturnRequestResponse>> getAllReturns(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = Messages.Validations.Pagination.PAGE_NUMBER_MIN) int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = Messages.Validations.Pagination.PAGE_SIZE_MIN) int pageSize,
            @RequestParam(defaultValue = "cDate") String sortBy,
            @RequestParam(defaultValue = "DESC") @Pattern(regexp = "^(?i)(ASC|DESC)$", message = Messages.Validations.Pagination.SORT_DIRECTION_INVALID) String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir.toUpperCase()), sortBy);
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);

        return returnRequestService.getAllReturns(pageable);
    }

    /**
     * Belirli bir iade talebinin nedenini, eklenen açıklamaları ve ürün detaylarını
     * derinlemesine incelemek için bu endpoint kullanıyoruz.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DataResult<ReturnRequestResponse>> getReturnById(
            @PathVariable @Min(value = 1, message = Messages.Validations.Return.RETURN_ID_INVALID) Long id) {
        return ResponseEntity.ok(returnRequestService.getReturnByIdForAdmin(id));
    }

    /**
     * Bir iade talebi hakkında karar verildiğinde durumu buradan güncelleriz.
     * Bu güncelleme, siparişin finansal sürecini ve stok durumunu doğrudan etkileyen bir adımdır.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<DataResult<ReturnRequestResponse>> updateReturnStatus(
            @PathVariable @Min(value = 1, message = Messages.Validations.Return.RETURN_ID_INVALID) Long id,
            @Valid @RequestBody UpdateReturnStatusRequest request) {
        return ResponseEntity.ok(returnRequestService.updateReturnStatus(id, request));
    }
}