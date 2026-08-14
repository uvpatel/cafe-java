package com.cafepos.ordering.application;

import com.cafepos.ordering.api.AssignTableRequest;
import com.cafepos.ordering.api.OrderResponse;

import java.util.UUID;

public interface AssignTableUseCase {
    OrderResponse assignTable(UUID cafeId, UUID orderId, AssignTableRequest request);
}
