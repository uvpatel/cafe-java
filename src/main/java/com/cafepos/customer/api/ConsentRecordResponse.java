package com.cafepos.customer.api;

import java.time.Instant;
import java.util.UUID;

public record ConsentRecordResponse(
        UUID id,
        UUID cafeId,
        UUID customerId,
        String consentType,
        boolean granted,
        Instant grantedAt,
        Instant revokedAt,
        String ipAddress,
        Instant createdAt
) {
}
