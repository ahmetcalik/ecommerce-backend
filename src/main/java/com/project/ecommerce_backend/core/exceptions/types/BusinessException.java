package com.project.ecommerce_backend.core.exceptions.types;

/**
 * Uygulamanın iş mantığı katmanında meydana gelen kural ihlallerini ve alan özgü hataları temsil eden temel istisna sınıfıdır.
 * Sistemsel arızalardan farklı olarak beklenen iş akışı kesintilerini yönetmek için tasarlanmıştır; çalışma zamanı istisnası olarak fırlatıldığı için kodun okunabilirliğini korur ve merkezi hata yönetimi tarafından yakalanarak kullanıcı dostu uyarı mesajlarına dönüştürülür.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
