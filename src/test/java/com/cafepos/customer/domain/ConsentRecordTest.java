package com.cafepos.customer.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsentRecordTest {

    private final UUID cafeId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    @Test
    @DisplayName("Should create active consent record and allow revocation")
    void consentLifecycleSuccess() {
        ConsentRecord consent = new ConsentRecord(cafeId, customerId, "MARKETING_EMAIL", true, "192.168.1.1");

        assertThat(consent.getId()).isNotNull();
        assertThat(consent.getCafeId()).isEqualTo(cafeId);
        assertThat(consent.getCustomerId()).isEqualTo(customerId);
        assertThat(consent.getConsentType()).isEqualTo("MARKETING_EMAIL");
        assertThat(consent.isGranted()).isTrue();
        assertThat(consent.getGrantedAt()).isNotNull();
        assertThat(consent.getRevokedAt()).isNull();

        consent.revoke();
        assertThat(consent.isGranted()).isFalse();
        assertThat(consent.getRevokedAt()).isNotNull();

        assertThatThrownBy(consent::revoke)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Consent is already revoked");
    }
}
