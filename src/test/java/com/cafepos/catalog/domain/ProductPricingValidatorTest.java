package com.cafepos.catalog.domain;

import com.cafepos.shared.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductPricingValidatorTest {

    private ProductPricingValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ProductPricingValidator();
    }

    @Test
    @DisplayName("Should validate matching currency for cafe")
    void shouldValidateMatchingCurrency() {
        Money price = Money.of(10.00);
        validator.validatePriceForCafeCurrency(price, "USD");
    }

    @Test
    @DisplayName("Should throw exception when currency does not match cafe currency")
    void shouldThrowWhenCurrencyMismatches() {
        Money usdPrice = Money.of(10.00);
        assertThatThrownBy(() -> validator.validatePriceForCafeCurrency(usdPrice, "EUR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Price currency 'USD' does not match cafe currency 'EUR'");
    }

    @Test
    @DisplayName("Should calculate discounted price correctly")
    void shouldCalculateDiscountedPrice() {
        Money price = Money.of(100.00);
        Money discounted = validator.calculateDiscountedPrice(price, BigDecimal.valueOf(15)); // 15% off

        assertThat(discounted.amount()).isEqualByComparingTo(new BigDecimal("85.0000"));
        assertThat(discounted.currency()).isEqualTo(Currency.getInstance("USD"));
    }

    @Test
    @DisplayName("Should reject invalid discount percentages")
    void shouldRejectInvalidDiscountPercentage() {
        Money price = Money.of(50.00);

        assertThatThrownBy(() -> validator.calculateDiscountedPrice(price, BigDecimal.valueOf(-5)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> validator.calculateDiscountedPrice(price, BigDecimal.valueOf(105)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
