package com.cafepos.ordering.domain;

import com.cafepos.shared.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderLineTest {

    private final Currency usd = Currency.getInstance("USD");

    @Test
    @DisplayName("Should create OrderLine successfully and calculate line subtotal and line total")
    void createOrderLineSuccess() {
        OrderLine line = new OrderLine(
                null,
                null,
                java.util.UUID.randomUUID(),
                "Espresso",
                Money.of(3.50),
                2,
                true
        );

        assertThat(line.getId()).isNotNull();
        assertThat(line.getProductNameSnapshot()).isEqualTo("Espresso");
        assertThat(line.getUnitPriceSnapshotAmount()).isEqualByComparingTo("3.5000");
        assertThat(line.getQuantity()).isEqualTo(2);
        assertThat(line.getLineSubtotalAmount()).isEqualByComparingTo("7.0000");
        assertThat(line.getLineTotalAmount()).isEqualByComparingTo("7.0000");
        assertThat(line.isKitchenRouted()).isTrue();
    }

    @Test
    @DisplayName("Should update quantity and recalculate amounts")
    void updateQuantitySuccess() {
        OrderLine line = new OrderLine(
                null,
                null,
                java.util.UUID.randomUUID(),
                "Latte",
                Money.of(4.00),
                1,
                true
        );

        line.updateQuantity(3, usd);

        assertThat(line.getQuantity()).isEqualTo(3);
        assertThat(line.getLineSubtotalAmount()).isEqualByComparingTo("12.0000");
        assertThat(line.getLineTotalAmount()).isEqualByComparingTo("12.0000");
    }

    @Test
    @DisplayName("Should throw exception when creating line with invalid parameters")
    void invalidParametersOnCreation() {
        java.util.UUID productId = java.util.UUID.randomUUID();

        assertThatThrownBy(() -> new OrderLine(null, null, null, "Latte", Money.of(4.00), 1, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product ID must not be null");

        assertThatThrownBy(() -> new OrderLine(null, null, productId, " ", Money.of(4.00), 1, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product name snapshot must not be empty");

        assertThatThrownBy(() -> new OrderLine(null, null, productId, "Latte", Money.of(-1.00), 1, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unit price snapshot must not be negative");

        assertThatThrownBy(() -> new OrderLine(null, null, productId, "Latte", Money.of(4.00), 0, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be greater than zero");
    }

    @Test
    @DisplayName("Should throw exception when updating to zero or negative quantity")
    void invalidQuantityUpdate() {
        OrderLine line = new OrderLine(
                null,
                null,
                java.util.UUID.randomUUID(),
                "Cappuccino",
                Money.of(4.50),
                2,
                false
        );

        assertThatThrownBy(() -> line.updateQuantity(0, usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be greater than zero");

        assertThatThrownBy(() -> line.updateQuantity(-2, usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be greater than zero");
    }

    @Test
    @DisplayName("Should add modifiers and recalculate line subtotal with modifier price deltas")
    void addModifierRecalculatesSubtotal() {
        OrderLine line = new OrderLine(
                null,
                null,
                java.util.UUID.randomUUID(),
                "Cappuccino",
                Money.of(4.50),
                2,
                true
        );

        line.addModifier(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), "Extra Shot", new java.math.BigDecimal("1.0000"), usd);

        assertThat(line.getModifiers()).hasSize(1);
        assertThat(line.getLineSubtotalAmount()).isEqualByComparingTo("11.0000"); // (4.50 + 1.00) * 2
        assertThat(line.getLineTotalAmount()).isEqualByComparingTo("11.0000");
    }
}

