package com.project.ecommerce_backend.api.controllers;

import com.project.ecommerce_backend.business.abstracts.AddressService;
import com.project.ecommerce_backend.business.dtos.requests.address.AddressRequest;
import com.project.ecommerce_backend.business.dtos.responses.address.AddressDetailResponse;
import com.project.ecommerce_backend.business.dtos.responses.address.ListAddressResponse;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.core.utils.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bu controller, sistemdeki kullanıcıların (Müşteri, Satıcı veya Admin) teslimat ve fatura adreslerini yönettiğimiz ana kapıdır.
 * Kullanıcıların profilindeki adres defteri işlemlerini (ekleme, silme, güncelleme) burada güvenli bir şekilde yürütüyoruz.
 * Güvenlik tarafında 'USER', 'SELLER' ve 'ADMIN' rollerine izin verdik çünkü her seviyedeki kullanıcının kendi adreslerini yönetebilmesi gerekiyor.
 * Ancak veri sahipliği (ownership) kontrolünü servis katmanında sıkı tutuyoruz; yani biri başkasının adresini silmeye kalkarsa sistemimiz buna geçit vermeyecek şekilde tasarlandı.
 */
@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'SELLER', 'ADMIN')")
@Validated
public class AddressesController {

    private final AddressService addressService;

    /**
     * Belirli bir adresin tüm detaylarına ulaşmak için bu endpoint kullanıyoruz.
     * Özellikle adres güncelleme ekranlarında mevcut bilgileri ön yüze basmak için oldukça kritik.
     */
    @GetMapping("/{id}")
    public DataResult<AddressDetailResponse> getById(
            @PathVariable @Min(value = 1, message = Messages.Validations.Common.INVALID_ID) Long id) {
        return this.addressService.getById(id);
    }

    /**
     * Kullanıcının kayıtlı tüm adreslerini listeleriz.
     * Sayfalama desteği sayesinde, özellikle çok fazla adresi olan kurumsal kullanıcılar için performansı koruyor ve veriyi parça parça sunuyoruz.
     */
    @GetMapping
    public DataResult<List<ListAddressResponse>> getAll(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = Messages.Validations.Pagination.PAGE_NUMBER_MIN) int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = Messages.Validations.Pagination.PAGE_SIZE_MIN) int pageSize,
            @RequestParam(defaultValue = "cDate") String sortBy,
            @RequestParam(defaultValue = "DESC") @Pattern(regexp = "^(?i)(ASC|DESC)$", message = Messages.Validations.Pagination.SORT_DIRECTION_INVALID) String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir.toUpperCase()), sortBy);
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);

        return this.addressService.getAll(pageable);
    }

    /**
     * Sisteme yeni bir adres tanımı yapmak için bu metodu kullanıyoruz.
     * Gelen veriyi validasyon süzgecinden geçirip, kullanıcının adres defterine yeni bir kayıt olarak ekliyoruz.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DataResult<AddressDetailResponse> add(
            @Valid @RequestBody AddressRequest request) {
        return this.addressService.add(request);
    }

    /**
     * Mevcut bir adres üzerindeki değişiklikleri güncelleriz.
     * Burada ID üzerinden ilgili adresi bulup, sadece yetkili kullanıcının bu değişikliği yapmasına izin veriyoruz.
     */
    @PutMapping("/{id}")
    public DataResult<AddressDetailResponse> update(
            @PathVariable @Min(value = 1, message = Messages.Validations.Common.INVALID_ID) Long id,
            @Valid @RequestBody AddressRequest request) {
        return this.addressService.update(id, request);
    }

    /**
     * Kullanıcının artık kullanmadığı adres bilgisini sistemden kaldırmak için bu endpoint tetikleriz.
     * Güvenlik gereği, işlem öncesinde adresin gerçekten o kullanıcıya ait olup olmadığını mutlaka teyit ediyoruz.
     */
    @DeleteMapping("/{id}")
    public Result delete(
            @PathVariable @Min(value = 1, message = Messages.Validations.Common.INVALID_ID) Long id) {
        return this.addressService.delete(id);
    }
}