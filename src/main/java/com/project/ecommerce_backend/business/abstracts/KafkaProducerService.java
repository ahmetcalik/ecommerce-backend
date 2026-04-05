package com.project.ecommerce_backend.business.abstracts;

import com.project.ecommerce_backend.business.dtos.events.OrderCreatedEvent;

public interface KafkaProducerService {

    void sendOrderCreatedEvent(OrderCreatedEvent event);

}