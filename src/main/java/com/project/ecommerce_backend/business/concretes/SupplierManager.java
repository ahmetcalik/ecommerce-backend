package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.SupplierService;
import com.project.ecommerce_backend.business.helpers.CacheHelper;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.BusinessException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.entities.concretes.Supplier;
import com.project.ecommerce_backend.repositories.abstracts.SupplierRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Platformdaki tedarikçi ekosistemini yöneten ve satıcı bazlı operasyonların güvenliğini sağlayan servis katmanıdır.
 * Bu sınıf; tedarikçilerin sisteme kayıtlı bilgilerine erişim, aktiflik durumlarının denetlenmesi ve oturum açmış kullanıcının tedarikçi kimliğiyle eşleştirilmesi süreçlerinden sorumludur. Tedarikçi verileri sistem genelinde ürün ve sipariş süreçlerinde sıkça sorgulandığı için, hem kimlik numarası hem de oturum bazlı anahtarlar üzerinden Redis hafızasında yüksek performansla önbelleğe alınır.
 */
@Service
@AllArgsConstructor
public class SupplierManager implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final MessageService messageService;
    private final CacheHelper cacheHelper;

    /**
     * Belirtilen kimlik numarasına sahip tedarikçiyi tüm temel verileriyle birlikte veritabanından sorgular.
     * Bu metot; ürün detaylarının gösterilmesi veya sipariş kalemlerinin ilgili satıcıyla eşleştirilmesi gibi yoğun okuma gerektiren senaryolarda kullanılır. Performans kazanımı sağlamak amacıyla sorgu sonuçları tedarikçi kimliği üzerinden önbelleğe alınarak tekrarlı isteklerdeki işlem maliyeti düşürülür.
     *
     * @param id Sorgulanmak istenen tedarikçinin benzersiz kimlik numarasıdır.
     * @return Sistemde tanımlı olan ilgili tedarikçi nesnesini döner.
     * @throws RuntimeException Belirtilen kimlik numarasıyla eşleşen bir tedarikçi kaydı bulunamadığında fırlatılır.
     */
    @Override
    @Cacheable(value = "supplier", key = "#id")
    public Supplier getByIdAsEntity(Long id) {
        return this.supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(messageService.getMessageWithParams(
                        Messages.Supplier.SUPPLIER_NOT_FOUND_WITH_GIVEN_ID, id)));
    }

    /**
     * Sistemde oturum açmış olan aktif kullanıcının tedarikçi profilini doğrular ve geri döndürür.
     * Bu metot; satıcılara özel panel işlemlerinde, ürün ekleme veya sipariş yönetimi gibi sadece yetkili tedarikçilerin yapabileceği eylemlerde kimlik tespiti için kullanılır. Güvenlik katmanı olarak tedarikçinin aktif olup olmadığını denetler ve sonuçları kullanıcı kimliğiyle ilişkilendirilmiş özel bir anahtar üzerinden önbelleğe alır.
     *
     * @return Giriş yapmış kullanıcıyla ilişkili olan ve aktif durumdaki tedarikçi nesnesini döner.
     * @throws BusinessException Kullanıcıya ait bir tedarikçi profili bulunamazsa veya profil pasif durumdaysa fırlatılır.
     */
    @Override
    @Cacheable(value = "supplier", key = "'authenticated:' + @cacheHelper.getAuthenticatedUserId()")
    public Supplier getAuthenticatedSupplierAsEntity() {

        Long authenticatedUserId = cacheHelper.getAuthenticatedUserId();

        Supplier supplier = supplierRepository.findByCUser(authenticatedUserId)
                .orElseThrow(() -> new BusinessException(messageService.getMessage(
                        Messages.Supplier.SUPPLIER_NOT_FOUND_FOR_AUTHENTICATED_USER)));

        checkIfSupplierIsActive(supplier);
        return supplier;
    }

    /**
     * Tedarikçinin sistem üzerindeki operasyonel haklarının devam edip etmediğini kontrol eden emniyet mekanizmasıdır.
     * Hesabı dondurulmuş veya kapatılmış tedarikçilerin sisteme veri girişi yapmasını veya mevcut siparişleri yönetmesini engelleyerek platform güvenliğini korur.
     */
    private void checkIfSupplierIsActive(Supplier supplier) {
        if (!Boolean.TRUE.equals(supplier.getIsActive())) {
            throw new BusinessException(messageService.getMessage(
                    Messages.Supplier.SUPPLIER_IS_NOT_ACTIVE));
        }
    }
}