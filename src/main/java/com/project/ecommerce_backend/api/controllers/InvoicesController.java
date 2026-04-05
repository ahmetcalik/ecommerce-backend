package com.project.ecommerce_backend.api.controllers;

import com.project.ecommerce_backend.business.abstracts.InvoiceService;
import com.project.ecommerce_backend.business.dtos.responses.invoice.ListUserInvoicesResponse;
import com.project.ecommerce_backend.business.dtos.responses.invoice.UserInvoiceDetailResponse;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bu controller, sistemdeki finansal kayıtların ve faturaların yönetim merkezidir.
 * Kullanıcılarımızın gerçekleştirdiği başarılı siparişler sonrası oluşan yasal dökümanlara buradan ulaşmalarını sağlıyoruz.
 * Güvenlik ve gizlilik gereği, her kullanıcı sadece kendi adına düzenlenmiş faturaları görebilir; bu yetki kontrolünü hem rol bazlı hem de servis katmanındaki sahiplik denetimleriyle sıkı bir şekilde sağlıyoruz.
 */
@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Validated
public class InvoicesController {

    private final InvoiceService invoiceService;

    /**
     * Oturum açmış kullanıcının sistemde kayıtlı olan tüm faturalarını tarih sırasına göre listeleriz.
     * Sayfalama yapısı sayesinde kullanıcının yıllar içindeki tüm alışveriş belgelerini performansı düşürmeden, düzenli bir şekilde sunabiliyoruz.
     */
    @GetMapping
    public DataResult<List<ListUserInvoicesResponse>> getAllInvoices(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = Messages.Validations.Pagination.PAGE_NUMBER_MIN) int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = Messages.Validations.Pagination.PAGE_SIZE_MIN) int pageSize,
            @RequestParam(defaultValue = "invoiceDate") String sortBy,
            @RequestParam(defaultValue = "DESC") @Pattern(regexp = "^(?i)(ASC|DESC)$", message = Messages.Validations.Pagination.SORT_DIRECTION_INVALID) String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir.toUpperCase()), sortBy);
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);

        return invoiceService.getAllMyInvoices(pageable);
    }

    /**
     * Belirli bir faturanın kalem kalem detaylarını, KDV ve ödeme bilgilerini görüntülemek için bu endpoint kullanıyoruz.
     */
    @GetMapping("/{id}")
    public DataResult<UserInvoiceDetailResponse> getInvoiceById(
            @PathVariable @Min(value = 1, message = Messages.Validations.Common.INVALID_ID) Long id) {
        return invoiceService.getMyInvoiceById(id);
    }
}