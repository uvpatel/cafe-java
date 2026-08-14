package com.cafepos.shared.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Immutable value object representing monetary amounts with safe decimal arithmetic.
 */
public record Money(BigDecimal amount, Currency currency) {

    public static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;
    public static final Currency DEFAULT_CURRENCY = Currency.getInstance("USD");

    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        amount = amount.setScale(4, DEFAULT_ROUNDING);
    }

    public static Money of(double value) {
        return new Money(BigDecimal.valueOf(value), DEFAULT_CURRENCY);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount, DEFAULT_CURRENCY);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(BigDecimal factor) {
        return new Money(this.amount.multiply(factor), this.currency);
    }

    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot operate on different currencies: " 
                + this.currency + " and " + other.currency);
        }
    }
}
