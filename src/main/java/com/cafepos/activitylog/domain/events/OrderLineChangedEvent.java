package com.cafepos.activitylog.domain.events;

import java.time.Instant;
import java.util.UUID;

public record OrderLineChangedEvent(
        UUID cafeId,
        UUID actorEmployeeId,
        String actorEmail,
        UUID orderId,
        UUID lineId,
        String actionType,
        String detailsJson,
        String ipAddress,
        Instant occurredAt
) {
    public OrderLineChangedEvent {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
