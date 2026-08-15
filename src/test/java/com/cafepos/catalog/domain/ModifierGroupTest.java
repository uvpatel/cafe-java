package com.cafepos.catalog.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModifierGroupTest {

    private final UUID cafeId = UUID.randomUUID();

    @Test
    @DisplayName("Should create ModifierGroup with valid properties")
    void createModifierGroupSuccess() {
        ModifierGroup group = new ModifierGroup(cafeId, "Milk Choice", 1, 1);

        assertThat(group.getId()).isNotNull();
        assertThat(group.getCafeId()).isEqualTo(cafeId);
        assertThat(group.getName()).isEqualTo("Milk Choice");
        assertThat(group.getMinSelection()).isEqualTo(1);
        assertThat(group.getMaxSelection()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when maxSelection < minSelection")
    void invalidSelectionLimitsThrowException() {
        assertThatThrownBy(() -> new ModifierGroup(cafeId, "Toppings", 2, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Maximum selection cannot be less than minimum selection");
    }
}
