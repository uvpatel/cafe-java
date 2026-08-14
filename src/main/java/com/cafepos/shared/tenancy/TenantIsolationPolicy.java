package com.cafepos.shared.tenancy;

import com.cafepos.shared.error.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

/**
 * Service/Repository policy checker for verifying multi-tenant isolation boundaries.
 */
@Component
public class TenantIsolationPolicy {

    /**
     * Verifies that an active tenant context exists and matches the expected tenant ID.
     *
     * @param targetTenantId the tenant ID of the entity or resource being accessed
     */
    public void verifyTenantAccess(UUID targetTenantId) {
        Objects.requireNonNull(targetTenantId, "targetTenantId cannot be null");
        verifyTenantAccess(targetTenantId.toString());
    }

    /**
     * Verifies that an active tenant context exists and matches the expected tenant ID.
     *
     * @param targetTenantId the tenant ID string of the entity or resource being accessed
     */
    public void verifyTenantAccess(String targetTenantId) {
        Objects.requireNonNull(targetTenantId, "targetTenantId cannot be null");
        String currentTenantId = TenantContextHolder.getRequiredTenantId();
        if (!currentTenantId.equals(targetTenantId)) {
            throw new BusinessException(
                    "TENANT_ACCESS_DENIED",
                    String.format("Tenant mismatch: current tenant [%s] cannot access tenant [%s] resource", currentTenantId, targetTenantId),
                    403
            );
        }
    }

    /**
     * Ensures an active tenant context is present.
     *
     * @return current active tenant ID
     */
    public String requireActiveTenant() {
        return TenantContextHolder.getRequiredTenantId();
    }
}
