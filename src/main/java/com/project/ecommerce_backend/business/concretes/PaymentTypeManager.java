package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.PaymentTypeService;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.entities.concretes.PaymentType;
import com.project.ecommerce_backend.entities.enums.PaymentTypeEnum;
import com.project.ecommerce_backend.repositories.abstracts.PaymentTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Sistemde kabul edilen ödeme yöntemlerinin kategorik tanımlamalarını yöneten referans veri servisidir.
 * Bu sınıf; ödeme süreçlerinin hangi temel kanal üzerinden yürüyeceğini belirleyen "Payment Type" verilerine merkezi bir erişim noktası sunar. Verilerin doğası gereği sabit referans verisi niteliğinde olması sebebiyle, veritabanı maliyetini minimize etmek ve ödeme adımlarında hız kazanmak amacıyla tüm sorgu sonuçları ödeme türü isimleri üzerinden Redis üzerinde önbelleğe alınır.
 */
@Service
@RequiredArgsConstructor
public class PaymentTypeManager implements PaymentTypeService {

    private final PaymentTypeRepository paymentTypeRepository;
    private final MessageService messageService;

    /**
     * Verilen ödeme türü adına göre, sistemdeki somut ödeme tipi tanımını (Entity) getirir.
     * Bu metot; özellikle yeni bir ödeme yöntemi kaydedilirken veya bir siparişin ödeme altyapısı doğrulanırken ihtiyaç duyulan ana tanımlayıcı veriyi sağlar. Performans optimizasyonu kapsamında, enum olarak gelen durum ismi anahtar olarak kullanılarak sonuçlar Redis hafızasında saklanır; böylece finansal işlemlerin en yoğun olduğu anlarda bile veritabanına gitmeden milisaniyeler içinde yanıt üretilir.
     *
     * @param name Sorgulanmak istenen ödeme türünün standartlaştırılmış enum değeridir.
     * @return Sistemde tanımlı olan somut PaymentType entity nesnesini döner.
     * @throws NotFoundException Belirtilen isimle eşleşen bir ödeme türü tanımı sistemde mevcut değilse fırlatılır.
     */
    @Override
    @Cacheable(value = "paymentType", key = "#name.name()")
    public PaymentType getByName(PaymentTypeEnum name) {
        return paymentTypeRepository.findByTypeName(name)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.PaymentType.PAYMENT_TYPE_NOT_FOUND, name.name())));
    }
}