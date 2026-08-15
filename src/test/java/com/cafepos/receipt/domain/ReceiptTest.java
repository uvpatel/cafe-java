package com.cafepos.receipt.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cafepos.shared.money.Money;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReceiptTest {

    private final UUID cafeId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @Test
    @DisplayName("Should create immutable receipt aggregate successfully")
    void createReceiptSuccess() {
        Receipt receipt = new Receipt(
                UUID.randomUUID(),
                cafeId,
                orderId,
                "RCP-20260815-0001",
                Money.of(new BigDecimal("100.00")),
                Money.of(new BigDecimal("10.00")),
                Money.of(new BigDecimal("5.00")),
                Money.of(new BigDecimal("2.00")),
                Money.of(new BigDecimal("97.00")),
                "[{\"taxName\":\"VAT\",\"amount\":5.0}]"
        );

        assertThat(receipt.getId()).isNotNull();
        assertThat(receipt.getCafeId()).isEqualTo(cafeId);
        assertThat(receipt.getOrderId()).isEqualTo(orderId);
        assertThat(receipt.getReceiptNumber()).isEqualTo("RCP-20260815-0001");
        assertThat(receipt.getSubtotalAmount()).isEqualByComparingTo("100.0000");
        assertThat(receipt.getTotalAmount()).isEqualByComparingTo("97.0000");
        assertThat(receipt.getTaxBreakdownJson()).contains("VAT");
    }

    @Test
    @DisplayName("Should fail when creating receipt with null required parameters")
    void createReceiptValidation() {
        assertThatThrownBy(() -> new Receipt(UUID.randomUUID(), null, orderId, "RCP-1", Money.of(10), null, null, null, Money.of(10), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cafeId must not be null");
    }
}
