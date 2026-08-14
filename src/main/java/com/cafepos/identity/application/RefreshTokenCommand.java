package com.cafepos.identity.application;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenCommand(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {}
