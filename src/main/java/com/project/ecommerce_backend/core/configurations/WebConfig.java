package com.project.ecommerce_backend.core.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Uygulamanın ağ seviyesindeki erişim politikalarını ve kökenler arası kaynak paylaşımı kurallarını yöneten merkezi yapılandırma sınıfıdır.
 * Bu sınıf; tarayıcı tabanlı güvenlik mekanizmalarının, farklı sunuculardan veya portlardan gelen meşru HTTP isteklerini engellemesini önleyerek istemci ve sunucu arasındaki güvenli iletişim köprüsünü kurar.
 */
@Configuration
public class WebConfig {

    /**
     * İstemci tarafı uygulamalarının API kaynaklarına kesintisiz erişebilmesi için gerekli olan global erişim protokollerini tanımlayan yapılandırıcıdır.
     * Geliştirme ortamlarında yaygın olarak kullanılan React ve Angular gibi ön yüz teknolojilerinin varsayılan portlarını güvenilir kaynaklar listesine ekler. Ayrıca standart HTTP metotlarına ve kimlik doğrulama bilgilerini taşıyan hassas başlıklara izin vererek modern web mimarisine uygun ve esnek bir veri alışverişi ortamı sağlar.
     *
     * @return Erişim izinlerini ve kısıtlamalarını içeren yapılandırma nesnesini döner.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(
                                "http://localhost:3000",
                                "http://localhost:4200",
                                "http://localhost:5173"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}

