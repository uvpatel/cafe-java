package com.cafepos.activitylog.domain.events;

import java.time.Instant;
import java.util.UUID;

public record PosSessionOpenedEvent(
        UUID cafeId,
        UUID actorEmployeeId,
        String actorEmail,
        UUID sessionId,
        UUID terminalId,
        String detailsJson,
        String ipAddress,
        Instant occurredAt
) {
    public PosSessionOpenedEvent {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
