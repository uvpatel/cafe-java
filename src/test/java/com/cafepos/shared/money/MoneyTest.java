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
}
