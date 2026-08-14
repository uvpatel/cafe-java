package com.cafepos.kitchen.api;

import com.cafepos.kitchen.domain.KitchenTicketStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DispatchOrderToKitchenResponse(
        UUID ticketId,
        UUID cafeId,
        UUID orderId,
        String orderNumber,
        KitchenTicketStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<KitchenTicketItemResponse> items
) {
}
