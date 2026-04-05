package com.project.ecommerce_backend.core.configurations;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Veritabanı varlıkları (entities) üzerindeki oluşturulma ve güncellenme zamanlarını, ayrıca bu işlemleri yapan kullanıcıları otomatik olarak takip eden JPA Auditing mekanizmasının yapılandırma sınıfıdır.
 * Bu konfigürasyonun ana uygulama sınıfından ayrılmasının temel sebebi sadece web katmanına odaklanan izole testlerde, tam bir Jveritabanı bağlamına ihtiyaç duyulmadığı için gereksiz bean yüklemelerini ve olası başlatma hatalarını engellemektir.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware", dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfiguration {
}