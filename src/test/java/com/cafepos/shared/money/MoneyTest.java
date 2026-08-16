package com.cafepos.shared.money;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    @DisplayName("Should maintain 4 decimal places scale with HALF_UP rounding")
    void testScaleAndRounding() {
        Money m = Money.of(new BigDecimal("10.50556"));
        assertThat(m.amount()).isEqualTo(new BigDecimal("10.5056"));
    }

    @Test
    @DisplayName("Should add money with matching currency")
    void testAddition() {
        Money m1 = Money.of(15.50);
        Money m2 = Money.of(4.50);
        Money result = m1.add(m2);

        assertThat(result.amount()).isEqualTo(new BigDecimal("20.0000"));
    }

    @Test
    @DisplayName("Should throw exception when operating across different currencies")
    void testCurrencyMismatch() {
        Money usd = new Money(new BigDecimal("10.00"), Currency.getInstance("USD"));
        Money eur = new Money(new BigDecimal("10.00"), Currency.getInstance("EUR"));

        assertThatThrownBy(() -> usd.add(eur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot operate on different currencies");
    }

    @Test
    @DisplayName("Should divide money correctly and throw on division by zero")
    void testDivision() {
        Money m = Money.of(100.00);
        Money result = m.divide(new BigDecimal("4"));
        assertThat(result.amount()).isEqualTo(new BigDecimal("25.0000"));

        assertThatThrownBy(() -> m.divide(BigDecimal.ZERO))
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("Division by zero");
    }

    @Test
    @DisplayName("Should compare monetary amounts of same currency")
    void testComparison() {
        Money m1 = Money.of(50.00);
        Money m2 = Money.of(20.00);
        Money m3 = Money.of(-5.00);

        assertThat(m1.isGreaterThan(m2)).isTrue();
        assertThat(m2.isGreaterThan(m1)).isFalse();
        assertThat(m2.isLessThan(m1)).isTrue();
        assertThat(m1.isLessThan(m2)).isFalse();
        assertThat(m3.isNegative()).isTrue();
        assertThat(m1.isNegative()).isFalse();
    }
}
