package com.cafepos.shared.tenancy;

import com.cafepos.shared.error.BusinessException;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * Aspect that intercepts execution of @TenantScoped annotated classes and methods
 * to guarantee an active TenantContext is present.
 */
@Aspect
@Component
public class TenantScopedAspect {

    @Before("@within(com.cafepos.shared.tenancy.TenantScoped) || @annotation(com.cafepos.shared.tenancy.TenantScoped)")
    public void verifyTenantContext() {
        if (TenantContextHolder.getOptionalTenantId().isEmpty()) {
            throw new BusinessException("TENANT_CONTEXT_MISSING", "Tenant context is required for tenant-scoped operations", 403);
        }
    }
}
