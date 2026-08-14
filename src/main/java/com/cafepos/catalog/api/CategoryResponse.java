package com.cafepos.catalog.api;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        UUID cafeId,
        String name,
        String colorHex,
        int sortOrder,
        boolean archived,
        Instant createdAt,
        Instant updatedAt
) {
}
