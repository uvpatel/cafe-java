package com.cafepos.activitylog.domain.events;

import java.time.Instant;
import java.util.UUID;

public record ExportTriggeredEvent(
        UUID cafeId,
        UUID actorEmployeeId,
        String actorEmail,
        UUID jobId,
        String exportType,
        String format,
        String detailsJson,
        String ipAddress,
        Instant occurredAt
) {
    public ExportTriggeredEvent {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
