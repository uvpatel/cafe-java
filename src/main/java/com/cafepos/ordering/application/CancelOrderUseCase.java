package com.cafepos.ordering.application;

import com.cafepos.ordering.api.OrderResponse;

import java.util.UUID;

public interface CancelOrderUseCase {
    OrderResponse cancelOrder(UUID cafeId, UUID orderId);
}
