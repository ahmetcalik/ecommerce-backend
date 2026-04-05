package com.project.ecommerce_backend.core.configurations;

import com.project.ecommerce_backend.core.security.details.CustomerDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Veritabanı kayıtlarının tarihçesini ve sahipliğini otomatik olarak yöneten merkezi yapılandırma sınıfıdır.
 * Bu sınıf; Spring Data JPA Auditing altyapısını etkinleştirerek, sistem genelindeki her bir kayıt veya güncelleme işleminde ilgili varlıkların denetim alanlarını güvenlik bağlamından alınan güncel kullanıcı verileriyle otomatik olarak doldurur ve veri izlenebilirliğini garanti altına alır.
 */
@Configuration
public class AuditingConfiguration {

    /**
     * Veritabanı varlıklarının yaşam döngüsündeki değişiklikleri izleyen ve kayıt altına alan yapılandırma sınıfıdır.
     * Bu sınıf; Spring Data JPA Auditing mekanizmasını Spring Security ile entegre ederek, verilerin kim tarafından ve ne zaman oluşturulduğu veya güncellendiği bilgisini otomatik olarak işler. Sistem genelindeki veri bütünlüğünü ve izlenebilirliği sağlamak amacıyla, her veritabanı işlemine bir zaman damgası ve kullanıcı kimliği ekler.
     */
    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
                return Optional.of(3L);
            }

            if (authentication.getPrincipal() instanceof CustomerDetails userDetails) {
                return Optional.of(userDetails.getId());
            }

            return Optional.of(3L);
        };
    }

    /**
     * Denetim alanları için standartlaştırılmış zaman damgası sağlayan sağlayıcıdır.
     * Sistem genelindeki zaman dilimi farklılıklarından kaynaklanabilecek tutarsızlıkları önlemek adına, sunucu zamanını ve saat dilimi bilgisini içeren hassas bir tarih yapısı üreterek kayıtların kronolojik sıralamasını ve zamansal doğruluğunu garanti altına alır.
     */
    @Bean(name = "auditingDateTimeProvider")
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }
}

