package com.project.ecommerce_backend.api.controllers;

import com.project.ecommerce_backend.business.abstracts.ProductService;
import com.project.ecommerce_backend.business.dtos.requests.product.AddProductRequest;
import com.project.ecommerce_backend.business.dtos.requests.product.UpdateProductRequest;
import com.project.ecommerce_backend.business.dtos.responses.product.AddProductResponse;
import com.project.ecommerce_backend.business.dtos.responses.product.ListProductsResponse;
import com.project.ecommerce_backend.business.dtos.responses.product.ProductDetailResponse;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.core.utils.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Bu controller, mağazamızdaki ürünlerin vitrinini ve depo yönetimini kontrol ettiğimiz ana merkezdir.
 * Müşterilerimizin ürünleri keşfetmesi, detaylarını incelemesi ve yöneticilerimizin ürün portföyünü güncel tutması için gereken tüm işlemleri burada topladık.
 * Okuma amaçlı endpoint'ler performans için 'Slice' yapısıyla desteklenmiş ve herkese açık bırakılmıştır.
 * Yazma ve silme gibi kritik endpoint'ler ise yetkisiz müdahaleleri engellemek adına 'ADMIN' ve 'SELLER' rollerine sıkı bir şekilde kilitlenmiştir.
 */
@RestController
@RequestMapping("/api/v1/products")
@AllArgsConstructor
@Validated
public class ProductsController {

    private final ProductService productService;

    /**
     * Belirli bir ürünün teknik özelliklerini, fiyatını ve stok durumunu içeren tüm detaylarını getirir.
     * Bu endpoint, ürün kartına tıklandığında açılan detay sayfasını beslemek için tasarlanmıştır.
     */
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public DataResult<ProductDetailResponse> getById(
            @PathVariable @Min(value = 1, message = Messages.Validations.Common.INVALID_ID) Long id) {
        return this.productService.getById(id);
    }

    /**
     * Mağazadaki ürünleri kategori, tedarikçi veya isim bazlı arama filtreleriyle listeleriz.
     * Sayfalama ve esnek sıralama seçenekleri sayesinde, binlerce ürün arasından arananın kolayca bulunmasını sağlayan, vitrin tarafının en yoğun kullanılan endpoint'idir.
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public DataResult<Slice<ListProductsResponse>> getAll(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = Messages.Validations.Pagination.PAGE_NUMBER_MIN) int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = Messages.Validations.Pagination.PAGE_SIZE_MIN) int pageSize,
            @RequestParam(defaultValue = "cDate") String sortBy,
            @RequestParam(defaultValue = "DESC")  @Pattern(regexp = "^(?i)(ASC|DESC)$", message = Messages.Validations.Pagination.SORT_DIRECTION_INVALID) String sortDir,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String nameSearch
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir.toUpperCase()), sortBy);
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);

        return this.productService.getAllWithPagination(pageable, categoryId, supplierId, nameSearch);
    }

    /**
     * Envantere yeni ürünler eklemek için bu endpoint kullanılır.
     * Ürünlerin doğru kategorize edilmesi ve temel bilgilerinin eksiksiz girilmesi, sağlıklı bir satış süreci için bu aşamada validasyonlarla denetlenir.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public DataResult<AddProductResponse> add(
            @Valid @RequestBody AddProductRequest addProductRequest) {
        return this.productService.add(addProductRequest);
    }

    /**
     * Mevcut bir ürünün fiyat, açıklama veya stok gibi bilgilerini güncellemek için kullanılır.
     * Sadece ilgili satıcının veya sistem yöneticisinin bu değişikliği yapmasına izin vererek veri güvenliğini sağlıyoruz.
     */
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public DataResult<ProductDetailResponse> update(
            @PathVariable @Min(value = 1, message = Messages.Validations.Common.INVALID_ID) Long id,
            @Valid @RequestBody UpdateProductRequest updateProductRequest) {
        return this.productService.update(id, updateProductRequest);
    }

    /**
     * Satışı durdurulan veya envanterden çıkarılması gereken ürünleri sistemde pasif hale getirir.
     * Finansal kayıtların bozulmaması için genellikle fiziksel silme yerine bu endpoint üzerinden erişimi kısıtlamayı tercih ediyoruz.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public Result delete(
            @PathVariable @Min(value = 1, message = Messages.Validations.Common.INVALID_ID) Long id) {
        return this.productService.delete(id);
    }
}