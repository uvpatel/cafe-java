package com.cafepos.reporting.api;

import com.cafepos.reporting.domain.ExportStatus;
import com.cafepos.reporting.domain.ExportType;

import java.time.Instant;
import java.util.UUID;

public record ExportJobResponse(
        UUID id,
        UUID cafeId,
        ExportType exportType,
        String format,
        ExportStatus status,
        String downloadUrl,
        Instant createdAt,
        Instant completedAt
) {
}
