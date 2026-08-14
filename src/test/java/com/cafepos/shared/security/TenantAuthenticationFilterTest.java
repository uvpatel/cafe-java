package com.cafepos.shared.security;

import com.cafepos.identity.infrastructure.JwtTokenClaims;
import com.cafepos.identity.infrastructure.JwtTokenService;
import com.cafepos.shared.error.BusinessException;
import com.cafepos.shared.tenancy.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantAuthenticationFilterTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private FilterChain filterChain;

    private TenantAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final String email = "employee@cafe.com";
    private final String role = "ADMIN";

    @BeforeEach
    void setUp() {
        filter = new TenantAuthenticationFilter(jwtTokenService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should populate TenantContextHolder and SecurityContext when valid Bearer token is provided")
    void validBearerToken_populatesContexts() throws Exception {
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        JwtTokenClaims claims = new JwtTokenClaims(tenantId, employeeId, email, role, "ACCESS");
        when(jwtTokenService.parseToken(token)).thenReturn(claims);

        doAnswer(invocation -> {
            // Verify context inside filter execution
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(tenantId.toString());

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.isAuthenticated()).isTrue();
            assertThat(auth.getPrincipal()).isInstanceOf(UserPrincipal.class);

            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            assertThat(principal.tenantId()).isEqualTo(tenantId);
            assertThat(principal.employeeId()).isEqualTo(employeeId);
            assertThat(principal.email()).isEqualTo(email);
            assertThat(principal.role()).isEqualTo(role);
            assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");

            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        // Verify context cleanup after filter completion
        assertThat(TenantContextHolder.getTenantId()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should proceed filter chain without setting contexts when Authorization header is missing")
    void missingToken_proceedsChainWithoutPopulatingContexts() throws Exception {
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(TenantContextHolder.getTenantId()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should reject request with 401 UNAUTHORIZED when token has expired")
    void expiredToken_rejectsWith401() throws Exception {
        String token = "expired.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtTokenService.parseToken(token))
                .thenThrow(new BusinessException("EXPIRED_TOKEN", "Token has expired", 401));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("EXPIRED_TOKEN");
        verify(filterChain, never()).doFilter(any(), any());

        // Contexts must be cleared
        assertThat(TenantContextHolder.getTenantId()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should reject request with 401 UNAUTHORIZED when token signature is invalid")
    void invalidSignature_rejectsWith401() throws Exception {
        String token = "invalid.signature.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtTokenService.parseToken(token))
                .thenThrow(new BusinessException("INVALID_TOKEN", "Invalid token signature", 401));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("INVALID_TOKEN");
        verify(filterChain, never()).doFilter(any(), any());

        assertThat(TenantContextHolder.getTenantId()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should reject request with 401 when token type is REFRESH instead of ACCESS")
    void refreshToken_rejectsWith401() throws Exception {
        String token = "refresh.token";
        request.addHeader("Authorization", "Bearer " + token);

        JwtTokenClaims claims = new JwtTokenClaims(tenantId, employeeId, email, role, "REFRESH");
        when(jwtTokenService.parseToken(token)).thenReturn(claims);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("INVALID_TOKEN_TYPE");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Should reject request with 401 when Bearer header is empty string")
    void emptyBearerToken_rejectsWith401() throws Exception {
        request.addHeader("Authorization", "Bearer ");

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Should clear contexts in finally block even when filterChain throws exception")
    void filterChainThrowsException_clearsContextInFinally() throws Exception {
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        JwtTokenClaims claims = new JwtTokenClaims(tenantId, employeeId, email, role, "ACCESS");
        when(jwtTokenService.parseToken(token)).thenReturn(claims);

        doAnswer(invocation -> {
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(tenantId.toString());
            throw new ServletException("Simulated downstream filter error");
        }).when(filterChain).doFilter(any(), any());

        assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
                .isInstanceOf(ServletException.class)
                .hasMessage("Simulated downstream filter error");

        // Verify thread local cleanup was performed despite exception
        assertThat(TenantContextHolder.getTenantId()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
