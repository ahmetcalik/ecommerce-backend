package com.project.ecommerce_backend.core.exceptions.types;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Veri sahipliği ilkelerine aykırı işlem girişimlerini ve nesne düzeyindeki yetkilendirme hatalarını temsil eden iş mantığı istisnasıdır.
 * Standart rol tabanlı erişim kontrollerinden farklı olarak, kullanıcının sisteme giriş yapmış olmasına rağmen kendisine ait olmayan bir kaynak üzerinde değişiklik yapmaya çalıştığı senaryolarda devreye girer ve istemciye HTTP 403 durum kodu ile yasaklı işlem yanıtı döner.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AuthorizationBusinessException extends BusinessException {

    public AuthorizationBusinessException(String message) {
        super(message);
    }
}