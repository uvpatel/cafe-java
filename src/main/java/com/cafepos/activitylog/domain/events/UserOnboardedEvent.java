package com.cafepos.activitylog.domain.events;

import java.time.Instant;
import java.util.UUID;

public record UserOnboardedEvent(
        UUID cafeId,
        UUID actorEmployeeId,
        String actorEmail,
        UUID newEmployeeId,
        String newEmployeeEmail,
        String role,
        String detailsJson,
        String ipAddress,
        Instant occurredAt
) {
    public UserOnboardedEvent {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
