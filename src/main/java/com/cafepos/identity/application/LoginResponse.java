package com.cafepos.identity.application;

import com.cafepos.identity.domain.EmployeeRole;
import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UUID employeeId,
        UUID cafeId,
        String email,
        EmployeeRole role
) {}
