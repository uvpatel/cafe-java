package com.cafepos.payment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SplitPaymentTest {

    @Test
    @DisplayName("Should create valid split payment with tip")
    void createSplitPayment() {
        UUID cafeId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID payMethodId = UUID.randomUUID();

        SplitPayment split = new SplitPayment(cafeId, orderId, 1, payMethodId, new BigDecimal("25.50"), new BigDecimal("5.00"), "Alice");

        assertThat(split.getAmount()).isEqualTo(new BigDecimal("25.50"));
        assertThat(split.getTipAmount()).isEqualTo(new BigDecimal("5.00"));
        assertThat(split.getPayerName()).isEqualTo("Alice");
        assertThat(split.getStatus()).isEqualTo(SplitPayment.SplitStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should validate positive split amount and non-negative tip")
    void validateInputs() {
        UUID cafeId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID payMethodId = UUID.randomUUID();

        assertThatThrownBy(() -> new SplitPayment(cafeId, orderId, 1, payMethodId, BigDecimal.ZERO, BigDecimal.ZERO, "Bob"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be positive");
    }
}
