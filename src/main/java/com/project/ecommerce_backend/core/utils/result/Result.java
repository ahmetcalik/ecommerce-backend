package com.project.ecommerce_backend.core.utils.result;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Uygulamanın servis katmanından dönen tüm yanıtları standart bir protokol altında toplayan sonuç modelleri hiyerarşisidir.
 * Bu yapı; API yanıtlarını başarı durumu (success), açıklayıcı mesaj (message) ve taşınan veri (data) olmak üzere üç temel bileşende sarmalar.
 * İstemci tarafında (Frontend/Mobil) tutarlı bir hata yönetimi ve veri işleme mekanizması kurulmasına olanak tanıyarak, backend ile frontend arasındaki iletişim dilini tek tipleştirir.
 */
@Getter
@NoArgsConstructor
public class Result {
    public boolean success;
    public String message;

    public Result(boolean success) {
        this.success = success;
    }

    public Result(String message, boolean success) {
        this.message = message;
        this.success = success;
    }
}
