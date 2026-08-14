package com.cafepos.ordering.api;

import com.cafepos.ordering.domain.OrderType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateOrderRequest(
        @NotNull(message = "POS Session ID must not be null")
        UUID posSessionId,

        @NotNull(message = "Order type must not be null")
        OrderType orderType,

        UUID diningTableId,

        String notes,

        String currency
) {
}
