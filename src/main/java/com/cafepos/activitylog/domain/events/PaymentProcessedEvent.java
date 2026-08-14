package com.cafepos.activitylog.domain.events;

import java.time.Instant;
import java.util.UUID;

public record PaymentProcessedEvent(
        UUID cafeId,
        UUID actorEmployeeId,
        String actorEmail,
        UUID paymentId,
        UUID orderId,
        String paymentMethod,
        String detailsJson,
        String ipAddress,
        Instant occurredAt
) {
    public PaymentProcessedEvent {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
