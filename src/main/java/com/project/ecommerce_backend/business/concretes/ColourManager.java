package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.ColourService;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.entities.concretes.Colour;
import com.project.ecommerce_backend.repositories.abstracts.ColourRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Ürünlerin görsel niteliklerinden biri olan renk (Colour) bilgilerinin yönetimini ve veritabanı erişim süreçlerini koordine eden iş mantığı servisidir.
 * Bu sınıf; ürün varyasyonlarının tanımlanması için gerekli olan renk verilerini sunar ve yüksek trafikli ürün listeleme süreçlerinde veritabanı üzerindeki yükü hafifletmek amacıyla sorgu sonuçlarını Redis üzerinde eşsiz kimlik numaralarıyla (ID) önbelleğe alır.
 */
@Service
@AllArgsConstructor
public class ColourManager implements ColourService {

    private final ColourRepository colourRepository;
    private final MessageService messageService;

    /**
     * Veritabanında kayıtlı olan belirli bir renk bilgisini, sistem içi operasyonlarda ve ürün eşleşmelerinde kullanılmak üzere doğrudan entity formatında getirir.
     * Bu metot; özellikle bir ürünün renk varyantı tanımlanırken veya filtrelenirken ihtiyaç duyulan somut veri modelini sağlar; performans optimizasyonu kapsamında sorgulanan her renk kaydı kendi kimlik numarası üzerinden Redis üzerinde önbelleğe alınarak tekrarlı sorguların hızlandırılması garanti edilir.
     *
     * @param id Bilgileri sorgulanmak istenen renk kaydının veritabanındaki eşsiz kimlik numarasıdır.
     * @return Sorgulanan rengi temsil eden somut Colour entity nesnesini döner.
     * @throws NotFoundException Belirtilen kimlik numarasıyla eşleşen bir renk kaydı bulunamadığında, uluslararasılaştırma desteğine sahip bir hata mesajı eşliğinde fırlatılır.
     */
    @Override
    @Cacheable(value = "colour", key = "#id")
    public Colour getByIdAsEntity(Long id) {
        return this.colourRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.Colour.COLOUR_NOT_FOUND_WITH_GIVEN_ID, id)));
    }
}