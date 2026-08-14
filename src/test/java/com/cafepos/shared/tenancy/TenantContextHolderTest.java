package com.cafepos.shared.tenancy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextHolderTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Should set and retrieve TenantContext")
    void testSetAndGetContext() {
        TenantContext context = TenantContext.of("tenant-123");
        TenantContextHolder.setContext(context);

        assertThat(TenantContextHolder.getContext()).isEqualTo(context);
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("tenant-123");
        assertThat(TenantContextHolder.getOptionalTenantId()).contains("tenant-123");
        assertThat(TenantContextHolder.getRequiredTenantId()).isEqualTo("tenant-123");
    }

    @Test
    @DisplayName("Should set context via tenantId string")
    void testSetTenantId() {
        TenantContextHolder.setTenantId("tenant-abc");

        assertThat(TenantContextHolder.getTenantId()).isEqualTo("tenant-abc");
        assertThat(TenantContextHolder.getContext()).isEqualTo(TenantContext.of("tenant-abc"));
    }

    @Test
    @DisplayName("Should return empty/null when context is not set")
    void testUnsetContext() {
        assertThat(TenantContextHolder.getContext()).isNull();
        assertThat(TenantContextHolder.getTenantId()).isNull();
        assertThat(TenantContextHolder.getOptionalContext()).isEmpty();
        assertThat(TenantContextHolder.getOptionalTenantId()).isEmpty();

        assertThatThrownBy(TenantContextHolder::getRequiredTenantId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tenant context bound to current thread");
    }

    @Test
    @DisplayName("Should clear context from thread local")
    void testClearContext() {
        TenantContextHolder.setTenantId("tenant-to-clear");
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("tenant-to-clear");

        TenantContextHolder.clear();
        assertThat(TenantContextHolder.getContext()).isNull();
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Should throw exception for invalid tenant context initialization")
    void testInvalidTenantContext() {
        assertThatThrownBy(() -> TenantContext.of(null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> TenantContext.of(""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> TenantContext.of("   "))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> TenantContextHolder.setContext(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should maintain thread isolation across threads")
    void testThreadIsolation() throws InterruptedException {
        TenantContextHolder.setTenantId("main-thread-tenant");

        AtomicReference<String> otherThreadTenantId = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Thread thread = new Thread(() -> {
            otherThreadTenantId.set(TenantContextHolder.getTenantId());
            latch.countDown();
        });

        thread.start();
        boolean completed = latch.await(2, TimeUnit.SECONDS);

        assertThat(completed).isTrue();
        assertThat(otherThreadTenantId.get()).isNull();
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("main-thread-tenant");
    }
}
