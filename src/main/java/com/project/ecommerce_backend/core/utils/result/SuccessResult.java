package com.project.ecommerce_backend.core.utils.result;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * İşlemlerin başarıyla tamamlandığını açıkça belirten ve istemciye olumlu geri bildirim mesajı taşıyan sonuç modelidir.
 * Temel sonuç sınıfını "true" başarı bayrağı ile özelleştirerek, kod içerisinde niyetin daha okunaklı olmasını sağlar ve veri dönmeyecek olan (void dönen servisler gibi) başarılı işlemler için standart bir yanıt şablonu sunar.
 */
@Getter
@NoArgsConstructor
public class SuccessResult extends Result {

    public SuccessResult(String message) {
        super(message, true);
    }
}
