package com.project.ecommerce_backend.core.utils.result;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * İşlemlerin başarısızlıkla sonuçlandığını veya iş kurallarına takıldığını belirten hata sonuç modelidir.
 * Temel sonuç hiyerarşisini "false" başarı bayrağı ile özelleştirerek, istemciye (Frontend/Mobil) işlemin reddedildiğini standart bir formatta bildirir. Özellikle GlobalExceptionHandler tarafından yakalanan istisnaları, son kullanıcıya sunulacak anlamlı hata metinleriyle sarmalamak için kullanılır.
 */
@Getter
@NoArgsConstructor
public class ErrorResult extends Result {

    public ErrorResult(String message) {
        super(message, false);
    }
}
