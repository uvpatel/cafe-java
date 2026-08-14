package com.cafepos.activitylog.domain.events;

import java.time.Instant;
import java.util.UUID;

public record CouponRedeemedEvent(
        UUID cafeId,
        UUID actorEmployeeId,
        String actorEmail,
        UUID redemptionId,
        String couponCode,
        UUID orderId,
        String detailsJson,
        String ipAddress,
        Instant occurredAt
) {
    public CouponRedeemedEvent {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
