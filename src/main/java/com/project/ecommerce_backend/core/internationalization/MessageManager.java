package com.project.ecommerce_backend.core.internationalization;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

/**
 * Uygulamanın çoklu dil desteğini çalışma zamanında yöneten ve iş mantığı katmanını statik metinlerden izole eden merkezi çeviri motorudur.
 * Spring Framework'ün kaynak yönetimi altyapısını kullanarak gelen her HTTP isteğinin yerel ayarına uygun mesajları dinamik olarak çözümler ve kullanıcı deneyimini kişiselleştirir.
 */
@Service
public class MessageManager implements MessageService {

    private final MessageSource messageSource;

    @Autowired
    public MessageManager(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Verilen anahtar değere karşılık gelen yerelleştirilmiş metni mevcut oturumun dil bağlamına göre getiren temel erişim metodudur.
     * Kaynak dosyalarında ilgili anahtar bulunamadığı takdirde sistemin işleyişini bozmadan anahtarın kendisini geri döndüren güvenli bir hata tolerans mekanizmasına sahiptir.
     */
    @Override
    public String getMessage(String key) {
        try {
            return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            return key;
        }
    }

    /**
     * Dinamik içerik barındıran mesaj şablonlarını çalışma zamanında parametrelerle birleştirerek anlamlı bütünler oluşturan gelişmiş çeviri fonksiyonudur.
     * Hata mesajlarına veya bilgilendirme metinlerine kullanıcı adı, sayısal değerler veya tarih gibi değişkenleri entegre ederek duruma özel ve bağlamsal geri bildirimler üretilmesini sağlar.
     */
    @Override
    public String getMessageWithParams(String keyword, Object... params) {
        try {
            return messageSource.getMessage(keyword, params, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            return keyword;
        }
    }
}