package com.project.ecommerce_backend.core.configurations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

/**
 * Uygulamanın çoklu dil desteği altyapısını kuran ve yöneten merkezi yapılandırma sınıfıdır.
 * Bu sınıf; sistem genelindeki hata ve bilgilendirme mesajlarının farklı dillerde sunulabilmesi için gerekli kaynak dosyalarının yüklenmesini sağlar ve gelen HTTP isteklerindeki başlık bilgilerini analiz ederek kullanıcının tercih ettiği dilin dinamik olarak tespit edilmesine olanak tanır.
 */
@Configuration
public class MessageSourceConfiguration {

    /**
     * Uygulama içerisindeki metin tabanlı geri bildirimlerin kaynağını tanımlayan mesaj yönetim bileşenidir.
     * Sınıf yolunda bulunan özellik dosyalarını tarayarak dile özgü içerikleri belleğe yükler ve API'nin istemcilere gönderdiği yanıtların yerelleştirilmiş mesajlar içermesini sağlar.
     */
    @Bean
    public ResourceBundleMessageSource bundleMessageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        return messageSource;
    }

    /**
     * İstemcinin tercih ettiği dil ve bölge bilgisini HTTP istek başlıklarından çözümleyen strateji belirleyicisidir.
     * Standart dil başlığını denetleyerek yanıt dilini belirleyen bu mekanizma, istemci tarafından herhangi bir dil tercihi belirtilmediği durumlarda sistem varsayılanı olarak İngilizce dil seçeneğinin kullanılmasını garanti altına alır.
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver acceptHeaderLocaleResolver = new AcceptHeaderLocaleResolver();
        acceptHeaderLocaleResolver.setDefaultLocale(Locale.ENGLISH);
        return acceptHeaderLocaleResolver;
    }
}