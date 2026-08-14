package com.cafepos.ordering.application;

import com.cafepos.ordering.api.CreateOrderRequest;
import com.cafepos.ordering.api.OrderResponse;

import java.util.UUID;

public interface CreateOrderUseCase {
    OrderResponse createOrder(UUID cafeId, CreateOrderRequest request);
}
