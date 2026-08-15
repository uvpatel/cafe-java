package com.cafepos.identity.api;

import com.cafepos.identity.domain.ShiftStatus;

import java.time.Instant;
import java.util.UUID;

public record EmployeeShiftResponse(
        UUID id,
        UUID cafeId,
        UUID employeeId,
        Instant clockInAt,
        Instant clockOutAt,
        Instant breakStartAt,
        Instant breakEndAt,
        ShiftStatus status,
        String notes,
        long totalWorkedMinutes,
        Instant createdAt,
        Instant updatedAt
) {
}
