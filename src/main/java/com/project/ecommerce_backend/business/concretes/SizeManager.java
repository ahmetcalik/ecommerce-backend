package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.SizeService;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.entities.concretes.Size;
import com.project.ecommerce_backend.repositories.abstracts.SizeRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Ürünlerin beden ve numara gibi fiziksel varyasyonlarını tanımlayan referans verilerin yönetiminden sorumlu servis katmanıdır.
 * Bu sınıf; kıyafet bedenlerinden ayakkabı numaralarına kadar envanter sistemindeki tüm ölçü tanımlamalarına merkezi bir erişim noktası sunar. Beden bilgileri sistemde nadiren değişen sabit nitelikte veriler olduğu için, veritabanı maliyetini düşürmek ve ürün varyant eşleşmelerini hızlandırmak amacıyla tüm sorgu sonuçları kimlik numarası üzerinden Redis üzerinde önbelleğe alınır.
 */
@Service
@AllArgsConstructor
public class SizeManager implements SizeService {

    private final SizeRepository sizeRepository;
    private final MessageService messageService;

    /**
     * Belirtilen benzersiz kimlik numarasına sahip beden veya numara tanımını tüm detaylarıyla birlikte getirir.
     * Bu metot; özellikle ürün varyantlarının oluşturulması, stok takibi ve sepet süreçlerinde doğru ölçü biriminin doğrulanması için kullanılır. Verinin referans niteliğinde olması sebebiyle, kimlik numarası anahtar olarak kullanılarak sonuçlar Redis hafızasında saklanır; bu sayede envanter sorgularındaki yoğun trafik yüksek performansla karşılanır.
     *
     * @param id Sorgulanmak istenen beden veya numara tanımının benzersiz kimlik numarasıdır.
     * @return Sistemde tanımlı olan ilgili beden nesnesini döner.
     * @throws NotFoundException Belirtilen kimlik numarasıyla eşleşen bir beden tanımı bulunamadığında fırlatılır.
     */
    @Override
    @Cacheable(value = "size", key = "#id")
    public Size getByIdAsEntity(Long id) {
        return this.sizeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(messageService.getMessage(
                        Messages.Size.SIZE_NOT_FOUND)));
    }
}