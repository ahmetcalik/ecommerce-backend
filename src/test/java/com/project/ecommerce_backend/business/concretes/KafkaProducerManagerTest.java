package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.dtos.events.OrderCreatedEvent;
import com.project.ecommerce_backend.core.configurations.KafkaConfiguration;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaProducerManagerTest {

    @Mock
    private KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    @InjectMocks
    private KafkaProducerManager kafkaProducerManager;

    @Test
    void sendOrderCreatedEvent_ShouldSendEventSuccessfully() {
        // Arrange
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(1L);

        RecordMetadata recordMetadata = new RecordMetadata(
                new TopicPartition("test-topic", 0), 0, 0, 0, 0, 0
        );
        SendResult<String, OrderCreatedEvent> sendResult = new SendResult<>(null, recordMetadata);

        CompletableFuture<SendResult<String, OrderCreatedEvent>> future = CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        // Act
        kafkaProducerManager.sendOrderCreatedEvent(event);

        // Assert
        verify(kafkaTemplate).send(eq(KafkaConfiguration.TOPIC_ORDER_CREATED), eq("1"), eq(event));
    }

    @Test
    void sendOrderCreatedEvent_ShouldHandleFailure() {
        // Arrange
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(1L);

        CompletableFuture<SendResult<String, OrderCreatedEvent>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka error"));

        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        // Act
        kafkaProducerManager.sendOrderCreatedEvent(event);

        // Assert
        verify(kafkaTemplate).send(eq(KafkaConfiguration.TOPIC_ORDER_CREATED), eq("1"), eq(event));
    }
}