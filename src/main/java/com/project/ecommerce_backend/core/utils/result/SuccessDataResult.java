package com.project.ecommerce_backend.core.utils.result;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * İşlemlerin başarıyla tamamlandığını ve beraberinde bir veri içeriği döndürüldüğünü temsil eden sonuç modelidir.
 * Başarı bayrağını varsayılan olarak "true" değerine ayarlar. Servis katmanından dönen başarılı sonuçları, jenerik veri tipi desteğiyle birlikte standart bir formatta sarmalayarak istemci tarafına güvenli bir şekilde aktarır.
 */
@Getter
@NoArgsConstructor
public class SuccessDataResult<T> extends DataResult<T> {
    public SuccessDataResult(T data) {
        super(data, true);
    }

    public SuccessDataResult(T data, String message) {
        super(data, message, true);
    }

    public SuccessDataResult(String message) {
        super(null, message, true);
    }

}
