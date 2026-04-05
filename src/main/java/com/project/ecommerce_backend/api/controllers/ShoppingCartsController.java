package com.project.ecommerce_backend.api.controllers;

import com.project.ecommerce_backend.business.abstracts.ShoppingCartService;
import com.project.ecommerce_backend.business.dtos.requests.cart.AddCartItemRequest;
import com.project.ecommerce_backend.business.dtos.requests.cart.UpdateCartItemRequest;
import com.project.ecommerce_backend.business.dtos.responses.cart.CartAndSessionResponse;
import com.project.ecommerce_backend.business.dtos.responses.cart.CartResponse;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.core.utils.result.Result;
import com.project.ecommerce_backend.core.utils.result.SuccessDataResult;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Bu controller, kullanıcıların alışveriş sepeti süreçlerini yönettiğimiz ana merkezdir.
 * Hem kayıtlı kullanıcılar hem de sisteme henüz giriş yapmamış misafir ziyaretçiler için kesintisiz bir sepet deneyimi sunuyoruz.
 * Misafir kullanıcıları tarayıcılarına bıraktığımız bir SESSION-ID çerezi üzerinden tanıyor, kullanıcı giriş yaptığı anda bu sepeti hesabı ile ilişkilendiriyoruz.
 */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Validated
public class ShoppingCartsController {

    private final ShoppingCartService shoppingCartService;
    private final MessageService messageService;

    private static final String SESSION_ID_COOKIE = "SESSION-ID";

    /**
     * Kullanıcının güncel sepet içeriğini getiren endpoint.
     * Eğer kullanıcı misafir ise çerezdeki ID üzerinden, giriş yapmış ise hesabındaki veri üzerinden sepet detaylarını döner.
     * Yeni misafirler için bu aşamada bir oturum kimliği oluşturulur.
     */
    @GetMapping
    public DataResult<CartResponse> getCart(
            @CookieValue(name = SESSION_ID_COOKIE, required = false) UUID sessionId,
            HttpServletResponse response) {

        CartAndSessionResponse cartAndSession = shoppingCartService.getCartDetails(sessionId);
        setSessionIdCookie(response, cartAndSession.getSessionId());

        return new SuccessDataResult<>(cartAndSession.getCartResponse(),
                messageService.getMessage(Messages.Cart.CART_FETCHED_SUCCESSFULLY));
    }

    /**
     * Sepete yeni bir ürün eklemek için kullanılan endpoint.
     * Ürün ekleme işlemi sırasında oturum takibini güncel tutarak, kullanıcının farklı sayfalarda gezse bile sepetini korumasını sağlarız.
     */
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public DataResult<CartResponse> addItemToCart(
            @CookieValue(name = SESSION_ID_COOKIE, required = false) UUID sessionId,
            @Valid @RequestBody AddCartItemRequest request,
            HttpServletResponse response) {

        CartAndSessionResponse cartAndSession = shoppingCartService.addItemToCart(sessionId, request);
        setSessionIdCookie(response, cartAndSession.getSessionId());

        return new SuccessDataResult<>(cartAndSession.getCartResponse(),
                messageService.getMessage(Messages.Cart.ITEM_ADDED_SUCCESSFULLY));
    }

    /**
     * Sepetteki mevcut bir ürünün miktarını güncellemek için kullanılan endpoint.
     * itemId parametresinin geçerliliği burada sıkı bir şekilde denetlenir.
     */
    @PutMapping("/items/{itemId}")
    public DataResult<CartResponse> updateItemQuantity(
            @CookieValue(name = SESSION_ID_COOKIE, required = false) UUID sessionId,
            @PathVariable @Min(value = 1, message = Messages.Validations.Common.INVALID_ID) Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            HttpServletResponse response) {

        CartAndSessionResponse cartAndSession = shoppingCartService.updateItemQuantity(sessionId, itemId, request);
        setSessionIdCookie(response, cartAndSession.getSessionId());

        return new SuccessDataResult<>(cartAndSession.getCartResponse(),
                messageService.getMessage(Messages.Cart.ITEM_UPDATED_SUCCESSFULLY));
    }

    /**
     * Belirli bir ürünü sepetten tamamen çıkarmak için gereken endpoint.
     * İşlem sonrası güncel sepet özeti kullanıcıya geri dönülür.
     */
    @DeleteMapping("/items/{itemId}")
    public DataResult<CartResponse> removeItemFromCart(
            @CookieValue(name = SESSION_ID_COOKIE, required = false) UUID sessionId,
            @PathVariable @Min(value = 1, message = Messages.Validations.Common.INVALID_ID) Long itemId,
            HttpServletResponse response) {

        CartAndSessionResponse cartAndSession = shoppingCartService.removeItemFromCart(sessionId, itemId);
        setSessionIdCookie(response, cartAndSession.getSessionId());

        return new SuccessDataResult<>(cartAndSession.getCartResponse(),
                messageService.getMessage(Messages.Cart.ITEM_REMOVED_SUCCESSFULLY));
    }

    /**
     * Kullanıcının sepetindeki tüm ürünleri tek seferde temizlemesini sağlayan endpoint.
     */
    @DeleteMapping
    public Result clearCart(
            @CookieValue(name = SESSION_ID_COOKIE, required = false) UUID sessionId) {
        return shoppingCartService.clearCart(sessionId);
    }

    /**
     * Misafir kullanıcılar için oluşturulan oturum kimliğini tarayıcıda saklamak üzere HTTP-Only çerez ayarlarını yapar.
     * Bu, sepetin 30 gün boyunca korunmasını sağlar.
     */
    private void setSessionIdCookie(HttpServletResponse response, UUID sessionId) {
        if (sessionId != null) {
            Cookie cookie = new Cookie(SESSION_ID_COOKIE, sessionId.toString());
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(60 * 60 * 24 * 30);
            response.addCookie(cookie);
        }
    }
}