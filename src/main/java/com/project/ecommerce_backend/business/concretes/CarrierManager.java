package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.CarrierService;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.entities.concretes.Carrier;
import com.project.ecommerce_backend.repositories.abstracts.CarrierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Sistemdeki lojistik operasyonların temel taşını oluşturan kargo firması (Carrier) bilgilerinin yönetiminden ve sorgulanmasından sorumlu servis katmanıdır.
 * Bu sınıf; siparişlerin fiziksel olarak müşteriye ulaştırılma sürecinde kullanılacak olan kargo şirketi verilerine güvenli bir şekilde erişim sağlar ve diğer iş birimleri (özellikle sipariş yönetimi) için kargo entegrasyon süreçlerinde ihtiyaç duyulan somut veri modellerini (Entity) sağlar.
 */
@Service
@RequiredArgsConstructor
public class CarrierManager implements CarrierService {

    private final CarrierRepository carrierRepository;
    private final MessageService messageService;

    /**
     * Veritabanında kayıtlı olan belirli bir kargo firmasının ham verisini (Entity), sistem içi operasyonlarda kullanılmak üzere doğrudan geri döndürür.
     * Bu metot; özellikle yeni bir sipariş oluşturulurken kargo firması seçiminin doğrulanması veya mevcut bir siparişin lojistik sağlayıcısının güncellenmesi gibi, veritabanı seviyesinde somut bir referansın (Foreign Key) gerekli olduğu senaryolarda kritik bir rol oynar; eğer talep edilen kimlik numarasıyla eşleşen bir firma bulunamazsa, sistem veri bütünlüğünü korumak adına uluslararasılaştırma desteğiyle zenginleştirilmiş bir hata mesajı eşliğinde istisna (Exception) fırlatır.
     *
     * @param id Sistem tarafından kargo firmasına atanmış olan eşsiz kimlik numarasıdır.
     * @return Sorgulanan kargo firmasını temsil eden somut Carrier entity nesnesini döner.
     * @throws NotFoundException Belirtilen kimlik numarasıyla eşleşen aktif bir kargo firması kaydı bulunamadığında tetiklenir.
     */
    @Override
    public Carrier getByIdAsEntity(Long id) {
        return carrierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.Carrier.CARRIER_NOT_FOUND_WITH_GIVEN_ID, id)));
    }
}