package com.project.ecommerce_backend.api.controllers;

import com.project.ecommerce_backend.business.abstracts.OrderService;
import com.project.ecommerce_backend.business.dtos.requests.order.UpdateOrderStatusRequest;
import com.project.ecommerce_backend.business.dtos.responses.order.OrderDetailResponse;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Bu controller, siparişlerin arka ofis yönetimini ve lojistik süreçlerini kontrol ettiğimiz merkezdir.
 * Müşteri siparişi verdikten sonra, satıcıların veya sistem yöneticilerinin paketi kargoya vermesi
 * veya teslim edildi olarak işaretlemesi gibi statü değişikliklerini burada gerçekleştiriyoruz.
 * Güvenlik gereği bu endpoint sadece 'SELLER' ve 'ADMIN' rollerine sahip yetkililer erişebilir;
 * yani burası son kullanıcının değil, operasyonu yürüten ekibin yönetim panelidir.
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
@Validated
public class AdminOrdersController {

    private final OrderService orderService;

    /**
     * Hazırlanan siparişin kargo bilgilerini sisteme girmek ve durumunu "Kargoda" olarak güncellemek için kullanılır.
     */
    @PatchMapping("/{orderId}/ship")
    public DataResult<OrderDetailResponse> shippingOrder(
            @PathVariable @Min(value = 1, message = Messages.Validations.Order.ORDER_ID_INVALID) Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.shippingOrder(orderId, request);
    }

    /**
     * Kurye veya kargo firmasından gelen onayla birlikte siparişi "Teslim Edildi" statüsüne çekeriz.
     * Bu adım, satış döngüsünün başarıyla tamamlandığını ve ürünün artık müşterinin elinde olduğunu sisteme işler.
     */
    @PatchMapping("/{orderId}/deliver")
    public DataResult<OrderDetailResponse> deliveryOrder(
            @PathVariable @Min(value = 1, message = Messages.Validations.Order.ORDER_ID_INVALID) Long orderId) {
        return orderService.deliveryOrder(orderId);
    }
}