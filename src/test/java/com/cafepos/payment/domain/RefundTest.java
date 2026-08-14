package com.cafepos.payment.domain;

import com.cafepos.shared.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefundTest {

    private final UUID cafeId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();

    @Test
    @DisplayName("Should successfully create refund record")
    void shouldCreateRefund() {
        Money amount = Money.of(BigDecimal.valueOf(20.00));
        String reason = "Customer requested cancellation";

        Refund refund = new Refund(cafeId, paymentId, amount, reason, employeeId);

        assertThat(refund.getId()).isNotNull();
        assertThat(refund.getCafeId()).isEqualTo(cafeId);
        assertThat(refund.getPaymentId()).isEqualTo(paymentId);
        assertThat(refund.getAmount()).isEqualTo(BigDecimal.valueOf(20.00).setScale(4));
        assertThat(refund.getReason()).isEqualTo(reason);
        assertThat(refund.getRefundedByEmployeeId()).isEqualTo(employeeId);
    }

    @Test
    @DisplayName("Should throw exception when refund reason is blank")
    void shouldThrowExceptionWhenReasonIsBlank() {
        Money amount = Money.of(BigDecimal.valueOf(10.00));

        assertThatThrownBy(() -> new Refund(cafeId, paymentId, amount, "   ", employeeId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Refund reason must not be empty");
    }

    @Test
    @DisplayName("Should throw exception when refund amount is zero or negative")
    void shouldThrowExceptionWhenAmountIsInvalid() {
        Money zeroAmount = Money.of(BigDecimal.ZERO);

        assertThatThrownBy(() -> new Refund(cafeId, paymentId, zeroAmount, "Reason", employeeId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Refund amount must be greater than zero");
    }
}
