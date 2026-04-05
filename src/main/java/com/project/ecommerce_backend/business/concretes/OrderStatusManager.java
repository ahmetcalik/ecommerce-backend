package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.OrderStatusService;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.entities.concretes.OrderStatus;
import com.project.ecommerce_backend.entities.enums.OrderStatusEnum;
import com.project.ecommerce_backend.repositories.abstracts.OrderStatusRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Siparişlerin yaşam döngüsü boyunca geçebileceği yasal ve mantıksal durum tanımlarını yöneten referans veri servisidir.
 * Bu sınıf; "Hazırlanıyor", "Kargolandı" veya "İptal Edildi" gibi statülerin veritabanındaki somut karşılıklarına (Entity) merkezi bir noktadan erişim sağlar. Durum tanımları sistemin işleyişi boyunca değişmeyen sabit nitelikte veriler olduğu için, tüm sorgular performans optimizasyonu amacıyla statü isimleri üzerinden Redis üzerinde önbelleğe alınır.
 */
@Service
public class OrderStatusManager implements OrderStatusService {

    private final OrderStatusRepository orderStatusRepository;
    private final MessageService messageService;
    private final OrderStatusService self;

    public OrderStatusManager(OrderStatusRepository orderStatusRepository,
                              MessageService messageService,
                              @Lazy OrderStatusService self) {
        this.orderStatusRepository = orderStatusRepository;
        this.messageService = messageService;
        this.self = self;
    }

    /**
     * Sisteme yeni girilen bir siparişin varsayılan olarak atandığı ilk aşama olan "Hazırlanıyor" (PREPARING) durumunu getirir.
     */
    @Override
    public OrderStatus getInitialStatus() {
        return self.findStatusByName(OrderStatusEnum.PREPARING);
    }

    /**
     * Siparişin lojistik sürece dahil edildiğini ve kargoya verildiğini temsil eden "Kargolandı" (SHIPPED) durumunu getirir.
     */
    @Override
    public OrderStatus getShippedStatus() {
        return self.findStatusByName(OrderStatusEnum.SHIPPED);
    }

    /**
     * Siparişin müşteriye başarıyla ulaştığını ve yaşam döngüsünün normal akışta sonlandığını ifade eden "Teslim Edildi" (DELIVERED) durumunu getirir.
     */
    @Override
    public OrderStatus getDeliveredStatus() {
        return self.findStatusByName(OrderStatusEnum.DELIVERED);
    }

    /**
     * Siparişin henüz kargoya verilmeden önce müşteri veya sistem tarafından durdurulduğunu temsil eden "İptal Edildi" (CANCELLED) durumunu getirir.
     */
    @Override
    public OrderStatus getCancelledStatus() {
        return self.findStatusByName(OrderStatusEnum.CANCELLED);
    }

    /**
     * Teslimat sonrası müşterinin iade talebinin onaylandığını ve ürünün geri alındığını belirten "İade Edildi" (RETURNED) durumunu getirir.
     */
    @Override
    public OrderStatus getReturnedStatus() {
        return self.findStatusByName(OrderStatusEnum.RETURNED);
    }

    /**
     * Verilen enum değeriyle eşleşen sipariş durumu verisini veritabanından veya önbellekten (Cache) yüksek performansla sorgular.
     * Bu metot; durum bilgilerinin sabit referans verisi olması sebebiyle statü isimlerini anahtar olarak kullanarak Redis üzerinde saklar; böylece sipariş süreçlerindeki yoğun statü sorgulamalarında veritabanı maliyetini ortadan kaldırır.
     * * @param statusName Sorgulanmak istenen durumun enum karşılığı.
     * @return İlgili durumu temsil eden somut OrderStatus entity nesnesini döner.
     * @throws NotFoundException İstenen isimle bir durum tanımı veritabanında mevcut değilse fırlatılır.
     */
    @Override
    @Cacheable(value = "orderStatus", key = "#statusName.name()")
    public OrderStatus findStatusByName(OrderStatusEnum statusName) {
        return orderStatusRepository.findByStatusName(statusName)
                .orElseThrow(() -> new NotFoundException(
                        messageService.getMessageWithParams(
                                Messages.OrderStatus.ORDER_STATUS_NOT_FOUND_BY_NAME, statusName.name())));
    }
}