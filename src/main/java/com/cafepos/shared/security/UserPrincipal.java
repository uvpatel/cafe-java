package com.cafepos.shared.security;

import java.io.Serializable;
import java.security.Principal;
import java.util.Objects;
import java.util.UUID;

/**
 * Security principal representing an authenticated user within a specific tenant scope.
 */
public record UserPrincipal(
        UUID tenantId,
        UUID employeeId,
        String email,
        String role
) implements Principal, Serializable {

    public UserPrincipal {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(employeeId, "employeeId cannot be null");
    }

    @Override
    public String getName() {
        return email != null ? email : employeeId.toString();
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}
