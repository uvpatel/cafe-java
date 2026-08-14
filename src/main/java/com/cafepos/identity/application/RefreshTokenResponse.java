package com.cafepos.identity.application;

public record RefreshTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {}
