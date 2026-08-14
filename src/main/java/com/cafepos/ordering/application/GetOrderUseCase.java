package com.cafepos.ordering.application;

import com.cafepos.ordering.api.OrderResponse;
import com.cafepos.ordering.domain.OrderStatus;

import java.util.List;
import java.util.UUID;

public interface GetOrderUseCase {
    OrderResponse getOrderById(UUID cafeId, UUID orderId);
    List<OrderResponse> getOrders(UUID cafeId, UUID posSessionId, OrderStatus status);
}
