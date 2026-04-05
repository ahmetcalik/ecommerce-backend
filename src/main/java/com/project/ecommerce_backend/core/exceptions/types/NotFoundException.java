package com.project.ecommerce_backend.core.exceptions.types;

import lombok.Getter;

/**
 * Veritabanında veya ilgili veri kaynağında talep edilen kaydın bulunamadığı durumları temsil eden özel istisna sınıfıdır.
 * Statik hata mesajlarının ötesine geçerek dinamik parametre desteği sunar; bu sayede hata yakalama katmanı, mesaj anahtarı ile birlikte gelen değişkenleri kullanarak çalışma zamanında detaylı ve kullanıcıya özgü bilgilendirme metinleri oluşturabilir.
 */
@Getter
@SuppressWarnings("squid:S1948")
public class NotFoundException extends RuntimeException {

    /**
     * Hata mesajının oluşturulması sırasında kullanılacak dinamik değerleri taşıyan argüman dizisidir.
     * Mesaj şablonundaki yer tutuculara karşılık gelen bu değerler, uluslararasılaştırma servisi tarafından işlenerek son kullanıcıya gösterilecek anlamlı metnin parçası haline getirilir.
     */
    private final Object[] args;

    public NotFoundException(String message) {
        super(message);
        this.args = null;
    }

    public NotFoundException(String message, Object... args) {
        super(message);
        this.args = args;
    }
}