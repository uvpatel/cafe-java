package com.cafepos.payment.domain;

import com.cafepos.shared.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    private final UUID cafeId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @Test
    @DisplayName("Should successfully create CASH payment and calculate change amount")
    void shouldCreateCashPaymentWithChange() {
        Money amount = Money.of(BigDecimal.valueOf(15.50));
        Money tendered = Money.of(BigDecimal.valueOf(20.00));

        Payment payment = new Payment(cafeId, orderId, PaymentMethod.CASH, amount, tendered, "REF-123");

        assertThat(payment.getId()).isNotNull();
        assertThat(payment.getCafeId()).isEqualTo(cafeId);
        assertThat(payment.getOrderId()).isEqualTo(orderId);
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(payment.getAmount()).isEqualTo(amount);
        assertThat(payment.getTenderedAmount()).isEqualTo(tendered);
        assertThat(payment.getChangeAmount()).isEqualTo(Money.of(BigDecimal.valueOf(4.50)));
        assertThat(payment.getTransactionReference()).isEqualTo("REF-123");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should successfully create CARD payment without tendered/change")
    void shouldCreateCardPayment() {
        Money amount = Money.of(BigDecimal.valueOf(25.00));

        Payment payment = new Payment(cafeId, orderId, PaymentMethod.CARD, amount, null, "TXN-999");

        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(payment.getAmount()).isEqualTo(amount);
        assertThat(payment.getTenderedAmount()).isNull();
        assertThat(payment.getChangeAmount()).isNull();
        assertThat(payment.getTransactionReference()).isEqualTo("TXN-999");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should throw exception when CASH payment has no tendered amount")
    void shouldThrowExceptionWhenCashPaymentHasNoTenderedAmount() {
        Money amount = Money.of(BigDecimal.valueOf(10.00));

        assertThatThrownBy(() -> new Payment(cafeId, orderId, PaymentMethod.CASH, amount, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tendered amount is required for CASH payment");
    }

    @Test
    @DisplayName("Should throw exception when CASH payment tendered amount is less than order amount")
    void shouldThrowExceptionWhenTenderedAmountIsInsufficient() {
        Money amount = Money.of(BigDecimal.valueOf(10.00));
        Money tendered = Money.of(BigDecimal.valueOf(8.00));

        assertThatThrownBy(() -> new Payment(cafeId, orderId, PaymentMethod.CASH, amount, tendered, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tendered amount cannot be less than order total amount");
    }

    @Test
    @DisplayName("Should throw exception when CASH payment tendered currency mismatched order currency")
    void shouldThrowExceptionWhenTenderedCurrencyMismatched() {
        Money amount = Money.of(new BigDecimal("10.00"), "USD");
        Money tenderedEUR = Money.of(new BigDecimal("20.00"), "EUR");

        assertThatThrownBy(() -> new Payment(cafeId, orderId, PaymentMethod.CASH, amount, tenderedEUR, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tendered amount currency mismatch");
    }

    @Test
    @DisplayName("Should mark payment as refunded")
    void shouldMarkPaymentAsRefunded() {
        Money amount = Money.of(BigDecimal.valueOf(10.00));
        Payment payment = new Payment(cafeId, orderId, PaymentMethod.CARD, amount, null, null);

        payment.markRefunded();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("Should throw exception when refunding an already refunded payment")
    void shouldThrowExceptionWhenRefundingAlreadyRefundedPayment() {
        Money amount = Money.of(BigDecimal.valueOf(10.00));
        Payment payment = new Payment(cafeId, orderId, PaymentMethod.CARD, amount, null, null);
        payment.markRefunded();

        assertThatThrownBy(payment::markRefunded)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Payment is already refunded");
    }

    @Test
    @DisplayName("Should throw exception for zero or negative payment amount")
    void shouldThrowExceptionForInvalidAmount() {
        Money zeroAmount = Money.of(BigDecimal.ZERO);

        assertThatThrownBy(() -> new Payment(cafeId, orderId, PaymentMethod.CARD, zeroAmount, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment amount must be greater than zero");
    }
}
