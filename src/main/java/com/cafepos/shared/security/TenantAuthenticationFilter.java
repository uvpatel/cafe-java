package com.cafepos.shared.security;

import com.cafepos.identity.infrastructure.JwtTokenClaims;
import com.cafepos.identity.infrastructure.JwtTokenService;
import com.cafepos.shared.error.BusinessException;
import com.cafepos.shared.tenancy.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Filter that authenticates requests using Bearer JWT tokens and populates both
 * Spring Security SecurityContext and TenantContextHolder.
 */
@Component
public class TenantAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;

    public TenantAuthenticationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = Objects.requireNonNull(jwtTokenService, "jwtTokenService must not be null");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader != null) {
                if (authHeader.startsWith(BEARER_PREFIX)) {
                    String token = authHeader.substring(BEARER_PREFIX.length()).trim();
                    if (token.isEmpty()) {
                        sendUnauthorizedResponse(request, response, "INVALID_TOKEN", "Bearer token is empty");
                        return;
                    }

                    try {
                        JwtTokenClaims claims = jwtTokenService.parseToken(token);
                        if (!"ACCESS".equalsIgnoreCase(claims.tokenType())) {
                            sendUnauthorizedResponse(request, response, "INVALID_TOKEN_TYPE", "Token is not an access token");
                            return;
                        }

                        if (claims.tenantId() != null) {
                            TenantContextHolder.setTenantId(claims.tenantId().toString());
                        }

                        UserPrincipal principal = new UserPrincipal(
                                claims.tenantId(),
                                claims.employeeId(),
                                claims.email(),
                                claims.role()
                        );

                        List<GrantedAuthority> authorities = claims.role() != null ?
                                List.of(new SimpleGrantedAuthority("ROLE_" + claims.role().toUpperCase())) :
                                Collections.emptyList();

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(principal, null, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } catch (BusinessException be) {
                        sendUnauthorizedResponse(request, response, be.getCode(), be.getMessage());
                        return;
                    } catch (Exception ex) {
                        sendUnauthorizedResponse(request, response, "INVALID_TOKEN", ex.getMessage());
                        return;
                    }
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void sendUnauthorizedResponse(HttpServletRequest request, HttpServletResponse response, String code, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = String.format(
                "{\"code\":\"%s\",\"message\":\"%s\",\"status\":401,\"detail\":\"%s\",\"path\":\"%s\"}",
                code,
                escapeJson(message),
                escapeJson(message),
                request.getRequestURI()
        );
        response.getWriter().write(body);
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\"", "\\\"");
    }
}
