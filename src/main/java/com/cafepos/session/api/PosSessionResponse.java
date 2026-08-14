package com.cafepos.session.api;

import com.cafepos.session.domain.PosSessionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PosSessionResponse(
        UUID id,
        UUID cafeId,
        UUID terminalId,
        UUID openedByEmployeeId,
        UUID closedByEmployeeId,
        PosSessionStatus status,
        BigDecimal openingBalanceAmount,
        BigDecimal closingExpectedAmount,
        BigDecimal closingCountedAmount,
        BigDecimal closingVarianceAmount,
        Instant openedAt,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
