package com.cafepos.activitylog.domain.events;

import java.time.Instant;
import java.util.UUID;

public record PosSessionClosedEvent(
        UUID cafeId,
        UUID actorEmployeeId,
        String actorEmail,
        UUID sessionId,
        String detailsJson,
        String ipAddress,
        Instant occurredAt
) {
    public PosSessionClosedEvent {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
