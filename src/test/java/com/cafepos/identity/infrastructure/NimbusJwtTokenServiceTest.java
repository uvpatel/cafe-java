package com.cafepos.identity.infrastructure;

import com.cafepos.shared.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NimbusJwtTokenServiceTest {

    private static final String SECRET_KEY = "test-secret-key-that-is-very-long-and-secure-for-hmac-sha256!";
    private static final long ACCESS_TOKEN_EXPIRATION = 900;
    private static final long REFRESH_TOKEN_EXPIRATION = 604800;

    private NimbusJwtTokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new NimbusJwtTokenService(SECRET_KEY, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION);
    }

    @Test
    @DisplayName("Should generate and parse valid access token")
    void shouldGenerateAndParseAccessToken() {
        UUID tenantId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        String email = "admin@cafepos.com";
        String role = "OWNER";

        String token = tokenService.generateAccessToken(tenantId, employeeId, email, role);

        assertThat(token).isNotBlank();
        assertThat(tokenService.validateToken(token)).isTrue();

        JwtTokenClaims claims = tokenService.parseToken(token);
        assertThat(claims.tenantId()).isEqualTo(tenantId);
        assertThat(claims.employeeId()).isEqualTo(employeeId);
        assertThat(claims.email()).isEqualTo(email);
        assertThat(claims.role()).isEqualTo(role);
        assertThat(claims.tokenType()).isEqualTo("ACCESS");
    }

    @Test
    @DisplayName("Should generate and parse valid refresh token")
    void shouldGenerateAndParseRefreshToken() {
        UUID tenantId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        String token = tokenService.generateRefreshToken(tenantId, employeeId);

        assertThat(token).isNotBlank();
        assertThat(tokenService.validateToken(token)).isTrue();

        JwtTokenClaims claims = tokenService.parseToken(token);
        assertThat(claims.tenantId()).isEqualTo(tenantId);
        assertThat(claims.employeeId()).isEqualTo(employeeId);
        assertThat(claims.tokenType()).isEqualTo("REFRESH");
    }

    @Test
    @DisplayName("Should reject token with invalid signature")
    void shouldRejectInvalidTokenSignature() {
        UUID tenantId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        String token = tokenService.generateAccessToken(tenantId, employeeId, "user@cafepos.com", "CASHIER");
        NimbusJwtTokenService otherService = new NimbusJwtTokenService("different-secret-key-that-is-also-32-bytes-long!", 900, 604800);

        assertThat(otherService.validateToken(token)).isFalse();
        assertThatThrownBy(() -> otherService.parseToken(token))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid token signature");
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken() {
        NimbusJwtTokenService expiredTokenService = new NimbusJwtTokenService(SECRET_KEY, -10, -10);
        UUID tenantId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        String expiredToken = expiredTokenService.generateAccessToken(tenantId, employeeId, "user@cafepos.com", "CASHIER");

        assertThat(tokenService.validateToken(expiredToken)).isFalse();
        assertThatThrownBy(() -> tokenService.parseToken(expiredToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Should reject malformed token string")
    void shouldRejectMalformedToken() {
        String malformedToken = "invalid.token.string";

        assertThat(tokenService.validateToken(malformedToken)).isFalse();
        assertThatThrownBy(() -> tokenService.parseToken(malformedToken))
                .isInstanceOf(BusinessException.class);
    }
}
