package com.project.ecommerce_backend.api.controllers;

import com.project.ecommerce_backend.business.abstracts.OrderService;
import com.project.ecommerce_backend.business.dtos.requests.order.AddOrderRequest;
import com.project.ecommerce_backend.business.dtos.responses.order.AddOrderResponse;
import com.project.ecommerce_backend.business.dtos.responses.order.ListUserOrdersResponse;
import com.project.ecommerce_backend.business.dtos.responses.order.UserOrderDetailResponse;
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
 * Bu controller, sistemin en hareketli noktası olan sipariş süreçlerini yönettiğimiz merkezdir.
 * Kullanıcıların sepetlerini birer alışverişe dönüştürmesi, geçmiş siparişlerini takip etmesi ve gerektiğinde iptal süreçlerini başlatması gibi tüm yaşam döngüsü burada işlenir.
 * Güvenlik bizim için çok kritik; bu yüzden hem rol bazlı erişimi denetliyoruz hem de her kullanıcının sadece kendi sipariş verisine dokunabildiğinden emin oluyoruz.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'SELLER', 'ADMIN')")
@Validated
public class OrdersController {

    private final OrderService orderService;

    /**
     * Kullanıcının geçmişten günümüze verdiği tüm siparişleri listeleriz.
     * Sayfalama desteği sayesinde, binlerce siparişi olan kurumsal müşterilerimiz bile listeyi yorulmadan ve performansı düşürmeden hızlıca görüntüleyebilir.
     */
    @GetMapping
    public DataResult<List<ListUserOrdersResponse>> getAllOrders(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = Messages.Validations.Pagination.PAGE_NUMBER_MIN) int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = Messages.Validations.Pagination.PAGE_SIZE_MIN) int pageSize,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "DESC") @Pattern(regexp = "^(?i)(ASC|DESC)$", message = Messages.Validations.Pagination.SORT_DIRECTION_INVALID) String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir.toUpperCase()), sortBy);
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);

        return orderService.getAllOrders(pageable);
    }

    /**
     * Belirli bir siparişin içeriğine; hangi ürünlerin alındığına, teslimat adresine ve ödeme özetine dair tüm detaylara bu endpoint üzerinden ulaşıyoruz.
     */
    @GetMapping("/{orderId}")
    public DataResult<UserOrderDetailResponse> getOrderDetailById(
            @PathVariable @Min(value = 1, message = Messages.Validations.Common.INVALID_ID) Long orderId) {
        return orderService.getOrderDetailById(orderId);
    }

    /**
     * Müşterinin sepetindeki ürünleri resmi bir siparişe dönüştürdüğümüz ana işlemdir.
     * Bu tetiklendiğinde stok kontrolleri yapılır, ödeme süreci doğrulanır ve lojistik hazırlıklar başlar.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DataResult<AddOrderResponse> add(
            @Valid @RequestBody AddOrderRequest request) {
        return orderService.add(request);
    }

    /**
     * Henüz kargoya verilmemiş veya iptal edilebilir durumdaki siparişleri durdurmak için kullanılır.
     * Bu işlem hem müşteri tarafından hem de operasyonel nedenlerle Admin tarafından tetiklenebilir; sonrasında genellikle iade süreçleri başlar.
     */
    @PostMapping("/{orderId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public Result cancelOrder(
            @PathVariable @Min(value = 1, message = Messages.Validations.Common.INVALID_ID) Long orderId) {
        return orderService.cancelOrder(orderId);
    }
}