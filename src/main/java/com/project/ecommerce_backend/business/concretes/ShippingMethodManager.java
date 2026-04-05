package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.ShippingMethodService;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.entities.concretes.ShippingMethod;
import com.project.ecommerce_backend.repositories.abstracts.ShippingMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Sistemde tanımlı olan kargo seçeneklerini ve lojistik yöntemlerini yöneten servis katmanıdır.
 * Bu sınıf; müşterilerin sipariş aşamasında seçebileceği farklı kargo firmaları ve teslimat türlerine ait teknik detaylara merkezi bir erişim noktası sağlar. Kargo yöntemleri ve bunlara bağlı ücretlendirme bilgileri sipariş hesaplamalarında kritik rol oynadığı için, veritabanı maliyetini düşürmek amacıyla tüm sorgu sonuçları yöntem kimliği üzerinden Redis üzerinde önbelleğe alınır.
 */
@Service
@RequiredArgsConstructor
public class ShippingMethodManager implements ShippingMethodService {

    private final ShippingMethodRepository shippingMethodRepository;
    private final MessageService messageService;

    /**
     * Belirtilen benzersiz numaraya sahip kargo yöntemini tüm detaylarıyla birlikte getirir.
     * Bu metot; özellikle sipariş oluşturma süreçlerinde kargo maliyetinin hesaplanması ve lojistik sağlayıcı bilgilerinin doğrulanması için kullanılır. Verinin referans niteliğinde olması sebebiyle, kimlik numarası anahtar olarak kullanılarak sonuçlar Redis hafızasında saklanır; bu sayede ödeme ve sepet sayfalarındaki yoğun sorgu trafiği yüksek performansla karşılanır.
     *
     * @param id Sorgulanmak istenen kargo yönteminin benzersiz kimlik numarasıdır.
     * @return Sistemde tanımlı olan ilgili kargo yöntemi nesnesini döner.
     * @throws NotFoundException Belirtilen kimlik numarasıyla eşleşen bir kargo yöntemi bulunamadığında fırlatılır.
     */
    @Override
    @Cacheable(value = "shippingMethod", key = "#id")
    public ShippingMethod getByIdAsEntity(Long id) {
        return shippingMethodRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.ShippingMethod.SHIPPING_METHOD_NOT_FOUND, id)));
    }
}