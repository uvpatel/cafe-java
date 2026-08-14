package com.cafepos.activitylog.domain.events;

import java.time.Instant;
import java.util.UUID;

public record RefundProcessedEvent(
        UUID cafeId,
        UUID actorEmployeeId,
        String actorEmail,
        UUID refundId,
        UUID paymentId,
        String detailsJson,
        String ipAddress,
        Instant occurredAt
) {
    public RefundProcessedEvent {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
