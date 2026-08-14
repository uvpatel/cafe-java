package com.cafepos.session.api;

import com.cafepos.session.domain.CashMovementType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CashMovementResponse(
        UUID id,
        UUID cafeId,
        UUID sessionId,
        UUID employeeId,
        CashMovementType type,
        BigDecimal amount,
        String reason,
        Instant createdAt
) {
}
