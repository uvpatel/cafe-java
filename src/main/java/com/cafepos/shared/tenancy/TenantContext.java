package com.cafepos.shared.tenancy;

import java.util.Objects;

/**
 * Immutable value object representing tenant identity context.
 */
public record TenantContext(String tenantId) {

    public TenantContext {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        if (tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId cannot be blank");
        }
    }

    public static TenantContext of(String tenantId) {
        return new TenantContext(tenantId);
    }
}
