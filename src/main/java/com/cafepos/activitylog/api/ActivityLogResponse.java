package com.cafepos.activitylog.api;

import java.time.Instant;
import java.util.UUID;

public record ActivityLogResponse(
        UUID id,
        UUID cafeId,
        UUID actorEmployeeId,
        String actorEmail,
        String action,
        String entityType,
        String entityId,
        String detailsJson,
        String ipAddress,
        Instant createdAt
) {
}
