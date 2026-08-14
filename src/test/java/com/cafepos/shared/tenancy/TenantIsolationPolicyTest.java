package com.cafepos.shared.tenancy;

import com.cafepos.shared.error.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantIsolationPolicyTest {

    private TenantIsolationPolicy policy;
    private final UUID tenant1 = UUID.randomUUID();
    private final UUID tenant2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        policy = new TenantIsolationPolicy();
        TenantContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Should pass verification when context tenant matches target resource tenant")
    void verifyTenantAccess_matchingTenant_succeeds() {
        TenantContextHolder.setTenantId(tenant1.toString());

        assertThatCode(() -> policy.verifyTenantAccess(tenant1)).doesNotThrowAnyException();
        assertThatCode(() -> policy.verifyTenantAccess(tenant1.toString())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw 403 BusinessException when context tenant does not match target tenant")
    void verifyTenantAccess_mismatchedTenant_throwsForbidden() {
        TenantContextHolder.setTenantId(tenant1.toString());

        assertThatThrownBy(() -> policy.verifyTenantAccess(tenant2))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tenant mismatch")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(403);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when no tenant context is set")
    void verifyTenantAccess_noContext_throwsException() {
        assertThatThrownBy(() -> policy.verifyTenantAccess(tenant1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tenant context bound to current thread");
    }

    @Test
    @DisplayName("Should return required tenant ID when context is present")
    void requireActiveTenant_returnsCurrentTenantId() {
        TenantContextHolder.setTenantId(tenant1.toString());

        assertThat(policy.requireActiveTenant()).isEqualTo(tenant1.toString());
    }
}
