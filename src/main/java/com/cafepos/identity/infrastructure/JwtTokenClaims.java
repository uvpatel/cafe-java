package com.cafepos.identity.infrastructure;

import java.util.UUID;

public record JwtTokenClaims(
        UUID tenantId,
        UUID employeeId,
        String email,
        String role,
        String tokenType
) {}
