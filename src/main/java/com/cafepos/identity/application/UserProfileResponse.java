package com.cafepos.identity.application;

import com.cafepos.identity.domain.EmployeeRole;
import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID employeeId,
        UUID cafeId,
        String fullName,
        String email,
        EmployeeRole role,
        boolean active,
        Instant lastLoginAt
) {}
