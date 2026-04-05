package com.project.ecommerce_backend.api.controllers;

import com.project.ecommerce_backend.business.abstracts.CategoryService;
import com.project.ecommerce_backend.business.dtos.requests.category.AddCategoryRequest;
import com.project.ecommerce_backend.business.dtos.requests.category.UpdateCategoryRequest;
import com.project.ecommerce_backend.business.dtos.responses.category.*;
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

import java.util.List;

/**
 * Bu controller, mağazamızdaki ürünlerin hiyerarşik düzenini yönettiğimiz yerdir.
 * Üst ve alt kategoriler arasındaki ilişkileri kurarak müşterilerimize düzenli bir alışveriş deneyimi sunarız.
 * Listeleme tüm ziyaretçilerimize açıkken, hiyerarşiyi değiştirecek olan ekleme, silme ve güncelleme yetkilerini sadece 'ADMIN' rolüne sahip yöneticilerimize saklıyoruz.
 */
@RestController
@RequestMapping("/api/v1/categories")
@AllArgsConstructor
@Validated
public class CategoriesController {

    private final CategoryService categoryService;

    /**
     * Mağazaya yeni bir kategori eklemek için kullanılır.
     * Eğer bu yeni kategori bir 'üst kategori'ye bağlıysa, bu ilişkiyi de burada tanımlayarak hiyerarşiye dahil ederiz.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public DataResult<AddCategoryResponse> add(
            @Valid @RequestBody AddCategoryRequest addCategoryRequest) {
        return this.categoryService.add(addCategoryRequest);
    }

    /**
     * Mevcut bir kategorinin ismini veya bağlı olduğu üst kategoriyi değiştirmek için bu endpoint kullanırız.
     */
    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public DataResult<UpdateCategoryResponse> update(
            @Valid @RequestBody UpdateCategoryRequest updateCategoryRequest) {
        return this.categoryService.update(updateCategoryRequest);
    }

    /**
     * Tüm kategorileri düz bir liste halinde, sayfalama ve sıralama seçenekleriyle birlikte sunarız.
     * Özellikle yönetim panellerinde tüm kategorileri listelemek için idealdir.
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public DataResult<Slice<ListCategoryResponse>> getAllWithPagination(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = Messages.Validations.Pagination.PAGE_NUMBER_MIN) int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = Messages.Validations.Pagination.PAGE_SIZE_MIN) int pageSize,
            @RequestParam(defaultValue = "cDate") String sortBy,
            @RequestParam(defaultValue = "DESC") @Pattern(regexp = "^(?i)(ASC|DESC)$", message = Messages.Validations.Pagination.SORT_DIRECTION_INVALID) String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir.toUpperCase()), sortBy);
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);
        return this.categoryService.getAllWithPagination(pageable);
    }

    /**
     * Kategorileri birbirine bağlı bir ağaç yapısı (parent-child) şeklinde döndürür.
     * Frontend navigasyon menüsü veya kategorize edilmiş filtreleme alanları oluştururken bu yapıyı kullanmak işleri çok kolaylaştırır.
     */
    @GetMapping("/getAllAsTree")
    @ResponseStatus(HttpStatus.OK)
    public DataResult<List<CategoryTreeResponse>> getAllAsTree() {
        return this.categoryService.getAllAsTree();
    }

    /**
     * Belirli bir kategorinin detay bilgilerini getirir.
     */
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public DataResult<CategoryDetailResponse> getById(
            @PathVariable @Min(value = 1, message = Messages.Validations.Category.CATEGORY_ID_INVALID) Long id) {
        return this.categoryService.getById(id);
    }

    /**
     * Bir kategoriyi sistemden kaldırmak için kullanılır.
     * Silerken, o kategoriye bağlı ürünlerin veya alt kategorilerin durumunu servis katmanındaki iş kurallarımızla denetliyoruz.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public Result delete(
            @PathVariable @Min(value = 1, message = Messages.Validations.Category.CATEGORY_ID_INVALID) Long id) {
        return this.categoryService.delete(id);
    }
}