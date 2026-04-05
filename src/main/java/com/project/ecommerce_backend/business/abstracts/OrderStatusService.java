package com.project.ecommerce_backend.business.abstracts;

import com.project.ecommerce_backend.entities.concretes.OrderStatus;
import com.project.ecommerce_backend.entities.enums.OrderStatusEnum;

public interface OrderStatusService {

    OrderStatus getInitialStatus();

    OrderStatus getShippedStatus();

    OrderStatus getDeliveredStatus();

    OrderStatus getCancelledStatus();

    OrderStatus getReturnedStatus();

    OrderStatus findStatusByName(OrderStatusEnum statusName);

}