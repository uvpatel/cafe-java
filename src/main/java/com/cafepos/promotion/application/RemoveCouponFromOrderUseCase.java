package com.cafepos.promotion.application;

import com.cafepos.ordering.api.OrderResponse;

import java.util.UUID;

public interface RemoveCouponFromOrderUseCase {

    OrderResponse removeCouponFromOrder(UUID cafeId, UUID orderId);
}
