package com.project.ecommerce_backend.core.utils.result;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * İşlemin başarısız olduğu durumlarda, hataya dair detaylı veri içeriğini ve açıklayıcı mesajı birlikte sarmalayan sonuç modelidir.
 * Başarı bayrağını varsayılan olarak "false" değerine ayarlar. Genellikle veri doğrulama (validation) hatalarında, kullanıcıya hangi alanların neden hatalı olduğunu listeleyen bir veri setiyle birlikte geri bildirim vermek amacıyla kullanılır.
 */
@Getter
@NoArgsConstructor
public class ErrorDataResult<T> extends DataResult<T>{
    public ErrorDataResult(T data) {
        super(data, false);
    }

    public ErrorDataResult(T data, String message) {
        super(data, message, false);
    }

    public ErrorDataResult(String message) {
        super(null, message, false);
    }

}
