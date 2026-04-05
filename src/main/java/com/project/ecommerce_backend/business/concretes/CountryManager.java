package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.CountryService;
import com.project.ecommerce_backend.business.dtos.responses.country.CountryResponse;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.utils.mapper.ModelMapperService;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.core.utils.result.SuccessDataResult;
import com.project.ecommerce_backend.entities.concretes.Country;
import com.project.ecommerce_backend.repositories.abstracts.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Sistem genelinde kullanılan coğrafi konum verilerinin ve ülke tanımlamalarının yönetimini üstlenen referans veri servisidir.
 * Bu sınıf; adres kayıtları, kargo maliyet hesaplamaları ve bölgesel kısıtlamalar için temel teşkil eden ülke verilerini sunar; verilerin doğası gereği nadiren değişmesi sebebiyle, tüm sorgu sonuçlarını Redis üzerinde uzun süreli önbelleğe alarak veritabanı üzerindeki tekrarlı sorgu maliyetini sıfıra indirmeyi hedefler.
 */
@Service
@RequiredArgsConstructor
public class CountryManager implements CountryService {

    private final CountryRepository countryRepository;
    private final ModelMapperService modelMapperService;
    private final MessageService messageService;

    /**
     * Sistemde tanımlı olan tüm ülkelerin listesini, kullanıcı arayüzlerinde (açılır listeler, kayıt formları vb.) kullanılmak üzere veri transfer nesneleri (DTO) formatında döner.
     * Ülke listesi sistem genelinde çok sık kullanılan ama içeriği neredeyse hiç değişmeyen bir "Static Reference Data" niteliğinde olduğu için, tüm liste tek bir anahtar ('all') altında Redis üzerinde saklanır ve bu sayede küresel adres formları gibi yüksek trafikli alanlarda maksimum yanıt hızı sağlanır.
     *
     * @return Başarıyla dönüştürülmüş ülke listesini, işlem onay mesajıyla birlikte bir DataResult kalıbı içinde sunar.
     */
    @Override
    @Cacheable(value = "country", key = "'all'")
    public DataResult<List<CountryResponse>> getAll() {
        List<Country> countries = countryRepository.findAll();
        List<CountryResponse> response = countries.stream()
                .map(country -> modelMapperService.getMapper().map(country, CountryResponse.class))
                .toList();
        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.Country.COUNTRY_SUCCESSFULLY_LISTED));
    }

    /**
     * Belirli bir ülkenin ham fiziksel bilgilerini, sistem içi iş mantığı doğrulamalarında ve adres ilişkilendirmelerinde kullanılmak üzere entity formatında getirir.
     * Bu metot; özellikle yeni bir adres oluşturulurken veya mevcut bir lokasyon güncellenirken veritabanı seviyesinde doğru ülke referansının kurulmasını sağlar; sorgulanan her ülke kaydı kendi eşsiz kimlik numarası (ID) üzerinden önbelleğe alınarak veri tutarlılığı ve hız dengesi korunur.
     *
     * @param id Bilgileri sorgulanmak istenen ülkenin veritabanındaki benzersiz anahtarıdır.
     * @return Sorgulanan ülkeyi temsil eden somut Country entity nesnesini döner.
     * @throws NotFoundException Belirtilen kimlik numarasıyla eşleşen bir ülke kaydı bulunamadığında, hata mesajı eşliğinde tetiklenir.
     */
    @Override
    @Cacheable(value = "country", key = "#id")
    public Country getByIdAsEntity(Long id) {
        return countryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.Country.COUNTRY_DOES_NOT_EXISTS_WITH_GIVEN_ID, id)));
    }
}