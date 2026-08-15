package com.cafepos.catalog.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModifierOptionTest {

    private final UUID groupId = UUID.randomUUID();

    @Test
    @DisplayName("Should create ModifierOption with valid attributes")
    void createModifierOptionSuccess() {
        ModifierOption option = new ModifierOption(groupId, "Oat Milk", new BigDecimal("0.7500"), true, 1);

        assertThat(option.getId()).isNotNull();
        assertThat(option.getGroupId()).isEqualTo(groupId);
        assertThat(option.getName()).isEqualTo("Oat Milk");
        assertThat(option.getPriceDelta()).isEqualByComparingTo("0.7500");
        assertThat(option.isDefault()).isTrue();
        assertThat(option.getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when price delta is negative")
    void negativePriceDeltaThrowsException() {
        assertThatThrownBy(() -> new ModifierOption(groupId, "Discount", new BigDecimal("-0.5000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Modifier price delta cannot be negative");
    }
}
