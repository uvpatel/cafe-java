package com.cafepos.ordering.application;

import com.cafepos.ordering.api.AddOrderLineRequest;
import com.cafepos.ordering.api.OrderResponse;
import com.cafepos.ordering.api.UpdateOrderLineRequest;

import java.util.UUID;

public interface ManageOrderLinesUseCase {
    OrderResponse addOrderLine(UUID cafeId, UUID orderId, AddOrderLineRequest request);
    OrderResponse updateOrderLine(UUID cafeId, UUID orderId, UUID lineId, UpdateOrderLineRequest request);
    OrderResponse deleteOrderLine(UUID cafeId, UUID orderId, UUID lineId);
}
