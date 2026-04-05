package com.project.ecommerce_backend.business.abstracts;

import com.project.ecommerce_backend.business.dtos.requests.order.AddOrderRequest;
import com.project.ecommerce_backend.business.dtos.requests.order.UpdateOrderStatusRequest;
import com.project.ecommerce_backend.business.dtos.responses.order.AddOrderResponse;
import com.project.ecommerce_backend.business.dtos.responses.order.ListUserOrdersResponse;
import com.project.ecommerce_backend.business.dtos.responses.order.OrderDetailResponse;
import com.project.ecommerce_backend.business.dtos.responses.order.UserOrderDetailResponse;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.core.utils.result.Result;
import com.project.ecommerce_backend.entities.concretes.Order;
import com.project.ecommerce_backend.entities.concretes.OrderItem;
import com.project.ecommerce_backend.entities.concretes.OrderStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {

    DataResult<UserOrderDetailResponse> getOrderDetailById(Long orderId);

    DataResult<List<ListUserOrdersResponse>> getAllOrders(Pageable pageable);

    DataResult<AddOrderResponse> add(AddOrderRequest addOrderRequest);

    Result cancelOrder(Long orderId);

    DataResult<OrderDetailResponse> shippingOrder(Long orderId, UpdateOrderStatusRequest request);

    DataResult<OrderDetailResponse> deliveryOrder(Long orderId);

    boolean existsByShippingAddressId(Long addressId);

    OrderItem findOrderItemByIdAndOrderIdAndCustomerId(Long orderItemId, Long orderId, Long customerId);

    OrderItem findOrderItemByIdAndCustomerId(Long orderItemId, Long customerId);

    Result updateOrderStatus(Long orderId, OrderStatus newStatus);

    Order findByIdAsEntityWithDetails(Long orderId);

}