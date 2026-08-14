package com.cafepos.shared.tenancy;

import com.cafepos.shared.error.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantScopedAspectTest {

    private TenantScopedAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new TenantScopedAspect();
        TenantContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Should pass aspect verification when tenant context is present")
    void verifyTenantContext_withActiveContext_succeeds() {
        TenantContextHolder.setTenantId(UUID.randomUUID().toString());

        assertThatCode(() -> aspect.verifyTenantContext()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw BusinessException with 403 when tenant context is missing")
    void verifyTenantContext_withoutContext_throwsForbidden() {
        assertThatThrownBy(() -> aspect.verifyTenantContext())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tenant context is required")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(403);
    }
}
