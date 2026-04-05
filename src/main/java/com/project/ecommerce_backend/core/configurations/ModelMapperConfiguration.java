package com.project.ecommerce_backend.core.configurations;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Veri transfer nesneleri ve veritabanı varlıkları arasındaki dönüşüm süreçlerini yöneten merkezi yapılandırma sınıfıdır.
 * Bu sınıf; katmanlar arası veri taşıma işlemlerinde kullanılan nesne eşleme aracını yapılandırarak, karmaşık veri yapılarının birbirine dönüştürülmesi sırasında oluşan kod tekrarlarını önler ve veri bütünlüğünü koruyan bir haritalama altyapısı sunar.
 */
@Configuration
public class ModelMapperConfiguration {

    /**
     * Nesne dönüşümleri için gerekli olan eşleme motorunu uygulama bağlamına dahil eden üretici metottur.
     * Alan isimleri ve veri türleri üzerinden akıllı eşleştirme stratejileri uygulayarak manuel veri kopyalama işlemini otomatize eder; böylece iş mantığı kodlarının veri dönüşüm detaylarından arındırılmasını ve daha temiz bir mimari yapının korunmasını sağlar.
     */
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}