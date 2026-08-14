package com.cafepos.kitchen.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record KitchenTicketDispatchedEvent(
        UUID ticketId,
        UUID cafeId,
        UUID orderId,
        String orderNumber,
        List<ItemDispatched> items,
        Instant dispatchedAt
) {
    public record ItemDispatched(
            UUID itemId,
            UUID orderLineId,
            UUID productId,
            String productName,
            int quantity
    ) {}
}
