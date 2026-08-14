package com.cafepos.kitchen.api;

import com.cafepos.kitchen.domain.KitchenItemStatus;

import java.time.Instant;
import java.util.UUID;

public record KitchenTicketItemResponse(
        UUID id,
        UUID ticketId,
        UUID orderLineId,
        UUID productId,
        String productName,
        int quantity,
        KitchenItemStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
