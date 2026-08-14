package com.cafepos.kitchen.application;

import com.cafepos.kitchen.api.DispatchOrderToKitchenResponse;

import java.util.UUID;

public interface DispatchOrderToKitchenUseCase {
    DispatchOrderToKitchenResponse dispatchOrderToKitchen(UUID cafeId, UUID orderId);
}
