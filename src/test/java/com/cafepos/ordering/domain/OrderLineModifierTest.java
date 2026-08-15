package com.cafepos.ordering.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderLineModifierTest {

    private final Currency usd = Currency.getInstance("USD");

    @Test
    @DisplayName("Should create OrderLineModifier successfully with valid snapshots")
    void createOrderLineModifierSuccess() {
        UUID orderLineId = UUID.randomUUID();
        UUID modifierGroupId = UUID.randomUUID();
        UUID modifierOptionId = UUID.randomUUID();

        OrderLineModifier modifier = new OrderLineModifier(
                orderLineId,
                modifierGroupId,
                modifierOptionId,
                " Oat Milk ",
                new BigDecimal("0.7500")
        );

        assertThat(modifier.getId()).isNotNull();
        assertThat(modifier.getOrderLineId()).isEqualTo(orderLineId);
        assertThat(modifier.getModifierGroupId()).isEqualTo(modifierGroupId);
        assertThat(modifier.getModifierOptionId()).isEqualTo(modifierOptionId);
        assertThat(modifier.getOptionNameSnapshot()).isEqualTo("Oat Milk");
        assertThat(modifier.getPriceDeltaSnapshotAmount()).isEqualByComparingTo("0.7500");
        assertThat(modifier.getPriceDeltaSnapshot(usd).amount()).isEqualByComparingTo("0.7500");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when inputs are invalid")
    void invalidInputsOnCreation() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> new OrderLineModifier(id, null, UUID.randomUUID(), "Oat Milk", BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Modifier group ID must not be null");

        assertThatThrownBy(() -> new OrderLineModifier(id, UUID.randomUUID(), null, "Oat Milk", BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Modifier option ID must not be null");

        assertThatThrownBy(() -> new OrderLineModifier(id, UUID.randomUUID(), UUID.randomUUID(), "  ", BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Option name snapshot must not be empty");

        assertThatThrownBy(() -> new OrderLineModifier(id, UUID.randomUUID(), UUID.randomUUID(), "Oat Milk", new BigDecimal("-0.50")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Price delta snapshot cannot be negative");
    }
}
