package com.cafepos.identity.infrastructure;

import com.cafepos.shared.error.BusinessException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class NimbusJwtTokenService implements JwtTokenService {

    private static final String ISSUER = "cafe-pos";
    private static final String CLAIM_TENANT_ID = "tenantId";
    private static final String CLAIM_EMPLOYEE_ID = "employeeId";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";

    private static final String TYPE_ACCESS = "ACCESS";
    private static final String TYPE_REFRESH = "REFRESH";

    private final byte[] secretKeyBytes;
    private final long accessTokenExpirationSeconds;
    private final long refreshTokenExpirationSeconds;

    public NimbusJwtTokenService(
            @Value("${jwt.secret:cafepos-super-secret-jwt-signing-key-minimum-256-bits-length!!}") String secretKey,
            @Value("${jwt.access-token-expiration-seconds:900}") long accessTokenExpirationSeconds,
            @Value("${jwt.refresh-token-expiration-seconds:604800}") long refreshTokenExpirationSeconds) {
        Objects.requireNonNull(secretKey, "secretKey must not be null");
        this.secretKeyBytes = deriveKeyBytes(secretKey);
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    private byte[] deriveKeyBytes(String secret) {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) {
            try {
                return MessageDigest.getInstance("SHA-256").digest(raw);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 algorithm not available", e);
            }
        }
        return raw;
    }

    @Override
    public String generateAccessToken(UUID tenantId, UUID employeeId, String email, String role) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(employeeId, "employeeId must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(role, "role must not be null");

        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(accessTokenExpirationSeconds);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(employeeId.toString())
                .claim(CLAIM_TENANT_ID, tenantId.toString())
                .claim(CLAIM_EMPLOYEE_ID, employeeId.toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuer(ISSUER)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiration))
                .build();

        return signJwt(claimsSet);
    }

    @Override
    public String generateRefreshToken(UUID tenantId, UUID employeeId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(employeeId, "employeeId must not be null");

        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(refreshTokenExpirationSeconds);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(employeeId.toString())
                .claim(CLAIM_TENANT_ID, tenantId.toString())
                .claim(CLAIM_EMPLOYEE_ID, employeeId.toString())
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuer(ISSUER)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiration))
                .build();

        return signJwt(claimsSet);
    }

    private String signJwt(JWTClaimsSet claimsSet) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                    .type(JOSEObjectType.JWT)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            JWSSigner signer = new MACSigner(secretKeyBytes);
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new BusinessException("JWT_GENERATION_FAILED", "Failed to generate JWT token: " + e.getMessage(), 500);
        }
    }

    @Override
    public JwtTokenClaims parseToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("INVALID_TOKEN", "Token string must not be empty", 401);
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(secretKeyBytes);

            if (!signedJWT.verify(verifier)) {
                throw new BusinessException("INVALID_TOKEN", "Invalid token signature", 401);
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Date expiration = claims.getExpirationTime();
            if (expiration != null && new Date().after(expiration)) {
                throw new BusinessException("EXPIRED_TOKEN", "Token has expired", 401);
            }

            String tenantIdStr = claims.getStringClaim(CLAIM_TENANT_ID);
            String employeeIdStr = claims.getStringClaim(CLAIM_EMPLOYEE_ID);
            if (employeeIdStr == null) {
                employeeIdStr = claims.getSubject();
            }
            String email = claims.getStringClaim(CLAIM_EMAIL);
            String role = claims.getStringClaim(CLAIM_ROLE);
            String type = claims.getStringClaim(CLAIM_TYPE);

            UUID tenantId = tenantIdStr != null ? UUID.fromString(tenantIdStr) : null;
            UUID employeeId = employeeIdStr != null ? UUID.fromString(employeeIdStr) : null;

            return new JwtTokenClaims(tenantId, employeeId, email, role, type);
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException("INVALID_TOKEN", "Failed to parse JWT token: " + e.getMessage(), 401);
        }
    }

    @Override
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }
}
