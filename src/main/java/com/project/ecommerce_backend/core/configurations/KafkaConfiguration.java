package com.project.ecommerce_backend.core.configurations;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka mesajlaşma altyapısını yapılandıran ve olay tabanlı iletişimin güvenilirliğini sağlayan merkezi konfigürasyon sınıfıdır.
 * Bu sınıf; konuların otomatik oluşturulması, geçici hatalarda mesajların yeniden işlenmesi ve çözülemeyen zehirli mesajların ölü mektup kuyruğuna iletilmesi süreçlerini yöneterek veri kaybını önleyen dayanıklı bir tüketici mimarisi kurar.
 */
@Configuration
public class KafkaConfiguration {

    public static final String TOPIC_ORDER_CREATED = "orders.created";
    public static final String TOPIC_ORDER_CREATED_DLT = TOPIC_ORDER_CREATED + ".DLT";

    /**
     * Sipariş oluşturma olaylarının yayınlanacağı ana Kafka konusunu programatik olarak oluşturur.
     * Sistem ölçeklenebilirliğini desteklemek amacıyla parçalı yapıda tasarlanan bu konu, yüksek hacimli sipariş trafiğini dağıtık mimariye uygun şekilde işlemek üzere yapılandırılmıştır.
     */
    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(TOPIC_ORDER_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * İşlenmesi sırasında kalıcı hata alınan veya veri bütünlüğü bozuk olan kayıtların izole edileceği ölü mektup kuyruğu konusunu tanımlar.
     * Ana akışı tıkayabilecek sorunlu mesajlar bu konuya yönlendirilerek sistemin sürekliliği sağlanır ve hatalı kayıtlar daha sonra incelenmek üzere güvenli bir alanda saklanır.
     */
    @Bean
    public NewTopic orderCreatedDltTopic() {
        return TopicBuilder.name(TOPIC_ORDER_CREATED_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }

    /**
     * Tüketici tarafında meydana gelen hataları yöneten ve sistemin hata toleransını artıran merkezi hata yakalama mekanizmasıdır.
     * Geçici ağ sorunları gibi durumlarda belirli aralıklarla yeniden deneme stratejisi uygular ve format bozukluğu gibi düzelmeyecek yapısal hatalarda mesajı doğrudan ölü mektup kuyruğuna yönlendirerek sonsuz döngüleri engeller.
     */
    @Bean
    public DefaultErrorHandler defaultErrorHandler(KafkaTemplate<String, Object> template) {

        DeadLetterPublishingRecoverer dltRecoverer = new DeadLetterPublishingRecoverer(template,
                (record, exception) -> new TopicPartition(TOPIC_ORDER_CREATED_DLT, record.partition())
        );

        FixedBackOff backOff = new FixedBackOff(2000L, 3L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(dltRecoverer, backOff);
        errorHandler.addNotRetryableExceptions(DeserializationException.class);

        return errorHandler;
    }

    /**
     * Kafka dinleyicilerini üreten fabrikayı özelleştirilmiş hata yönetimi politikalarıyla yapılandırır.
     * Standart dinleyici davranışını genişleterek tüm tüketici servislerinin tanımlanan yeniden deneme, bekleme ve hata ayıklama kurallarına otomatik olarak uymasını ve tutarlı bir mesaj işleme döngüsü sunmasını sağlar.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory,
            DefaultErrorHandler defaultErrorHandler) {

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);
        factory.setCommonErrorHandler(defaultErrorHandler);

        return factory;
    }
}