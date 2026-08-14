package com.cafepos.identity.infrastructure;

import java.util.UUID;

public interface JwtTokenService {

    String generateAccessToken(UUID tenantId, UUID employeeId, String email, String role);

    String generateRefreshToken(UUID tenantId, UUID employeeId);

    JwtTokenClaims parseToken(String token);

    boolean validateToken(String token);

    long getAccessTokenExpirationSeconds();
}
