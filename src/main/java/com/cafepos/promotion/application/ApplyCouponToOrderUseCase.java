package com.cafepos.promotion.application;

import com.cafepos.ordering.api.OrderResponse;
import com.cafepos.promotion.api.ApplyCouponRequest;

import java.util.UUID;

public interface ApplyCouponToOrderUseCase {

    OrderResponse applyCouponToOrder(UUID cafeId, UUID orderId, ApplyCouponRequest request);
}
