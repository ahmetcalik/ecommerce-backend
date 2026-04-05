package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.KafkaProducerService;
import com.project.ecommerce_backend.business.dtos.events.OrderCreatedEvent;
import com.project.ecommerce_backend.core.configurations.KafkaConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Sistem içerisindeki kritik olayların (events) mesaj kuyruğu altyapısına aktarılmasını sağlayan asenkron haberleşme servisidir.
 * Bu sınıf; uygulama içerisinde gerçekleşen önemli iş süreçlerini (örneğin sipariş oluşturulması), Kafka ekosistemi üzerinden diğer mikroservislerin tüketimine sunar. Veri iletimi sırasında "Idempotent Producer" prensiplerine uygun olarak mesajın güvenliğini ve sıralamasını garanti altına alırken, iletim sonuçlarını asenkron geri çağırma (callback) mekanizmalarıyla takip ederek sistemin sürekliliğini ve izlenebilirliğini (observability) sağlar.
 */
@Service
public class KafkaProducerManager implements KafkaProducerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaProducerManager.class);
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public KafkaProducerManager(KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Yeni bir sipariş oluşturulduğunda, bu olayı ilgili Kafka topic'ine asenkron bir şekilde iletir.
     * İletim işlemi sırasında sipariş numarası (OrderId) "Message Key" olarak kullanılır; bu sayede aynı siparişe ait tüm olayların Kafka üzerinde aynı partition'a gitmesi ve kronolojik sırasının korunması garanti edilir. Metot, iletim sonucunu bekleyip ana iş akışını bloklamak yerine, Java CompletableFuture altyapısını kullanarak sonucu arka planda (non-blocking) değerlendirir; başarılı iletimlerde partition ve offset bilgilerini, başarısız durumlarda ise detaylı hata loglarını sisteme işler.
     *
     * @param event Siparişin detaylarını ve durum bilgilerini barındıran olay (event) veri paketidir.
     */
    @Override
    public void sendOrderCreatedEvent(OrderCreatedEvent event) {
        LOGGER.info("Kafka'ya 'ORDER_CREATED' olayı gönderiliyor. OrderID: {}", event.getOrderId());

        CompletableFuture<SendResult<String, OrderCreatedEvent>> future = kafkaTemplate.send(
                KafkaConfiguration.TOPIC_ORDER_CREATED,
                event.getOrderId().toString(),
                event
        );

        future.whenComplete((result, exception) -> {
            if (exception != null) {
                LOGGER.error(
                        "Kafka'ya 'ORDER_CREATED' olayı GÖNDERİLEMEDİ! OrderID: {}. Hata: {}",
                        event.getOrderId(),
                        exception.getMessage()
                );
            } else {
                LOGGER.info(
                        "Kafka'ya 'ORDER_CREATED' olayı başarıyla GÖNDERİLDİ. OrderID: {}. [Topic: {}, Partition: {}, Offset: {}]",
                        event.getOrderId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()
                );
            }
        });
    }
}