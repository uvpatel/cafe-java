package com.cafepos.shared.tenancy;

import java.util.Objects;
import java.util.Optional;

/**
 * Thread-local holder for maintaining tenant context across execution boundaries.
 */
public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CONTEXT = new ThreadLocal<>();

    private TenantContextHolder() {
        // Utility class
    }

    public static void setContext(TenantContext context) {
        Objects.requireNonNull(context, "TenantContext cannot be null");
        CONTEXT.set(context);
    }

    public static void setTenantId(String tenantId) {
        setContext(TenantContext.of(tenantId));
    }

    public static TenantContext getContext() {
        return CONTEXT.get();
    }

    public static Optional<TenantContext> getOptionalContext() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static String getTenantId() {
        TenantContext ctx = CONTEXT.get();
        return ctx != null ? ctx.tenantId() : null;
    }

    public static Optional<String> getOptionalTenantId() {
        return getOptionalContext().map(TenantContext::tenantId);
    }

    public static String getRequiredTenantId() {
        return getOptionalTenantId()
                .orElseThrow(() -> new IllegalStateException("No tenant context bound to current thread"));
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
