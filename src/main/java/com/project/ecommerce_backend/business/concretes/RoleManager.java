package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.RoleService;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.entities.concretes.Role;
import com.project.ecommerce_backend.repositories.abstracts.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Sistemdeki kullanıcı rollerini yöneten ve yetkilendirme altyapısına temel teşkil eden servis katmanıdır.
 * Bu sınıf; sistemde tanımlı olan kullanıcı yetki seviyelerinin veritabanındaki karşılıklarına merkezi bir erişim noktası sunar. Roller, uygulamanın yaşam döngüsü boyunca nadiren değişen ve her istekte doğrulanan kritik veriler olduğu için, tüm sorgu sonuçları rol isimleri üzerinden Redis üzerinde önbelleğe alınarak sistemin yetkilendirme hızı optimize edilir.
 */
@Service
@RequiredArgsConstructor
public class RoleManager implements RoleService {

    private final RoleRepository roleRepository;
    private final MessageService messageService;

    /**
     * Verilen rol ismine göre sistemde tanımlı olan ilgili yetki tanımını getirir.
     * Bu metot; özellikle yeni kullanıcı kaydı sırasında varsayılan rollerin atanmasında veya mevcut kullanıcıların yetki seviyelerinin güncellenmesinde anahtar rol oynar. Rol isimleri eşsiz birer anahtar olarak kullanılarak sonuçlar Redis hafızasında saklanır; bu sayede güvenlik kontrolleri sırasında veritabanına ek yük bindirilmeden yüksek performanslı bir doğrulama süreci yürütülür.
     *
     * @param name Sorgulanmak istenen rolün standart ismidir.
     * @return Sistemde tanımlı olan somut Role nesnesini döner.
     * @throws NotFoundException Belirtilen isimle eşleşen bir rol tanımı sistemde mevcut değilse fırlatılır.
     */
    @Override
    @Cacheable(value = "role", key = "#name")
    public Role findRoleByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.Role.ROLE_NOT_FOUND, name
                )));
    }
}