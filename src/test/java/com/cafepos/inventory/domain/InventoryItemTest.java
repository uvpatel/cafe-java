package com.cafepos.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryItemTest {

    @Test
    @DisplayName("Should create inventory item and identify low stock")
    void createAndCheckLowStock() {
        UUID cafeId = UUID.randomUUID();
        InventoryItem item = new InventoryItem(cafeId, null, "SKU-001", "Coffee Beans", "KG", new BigDecimal("10.00"), new BigDecimal("15.00"), new BigDecimal("500.00"));

        assertThat(item.getSku()).isEqualTo("SKU-001");
        assertThat(item.isLowStock()).isTrue();
    }

    @Test
    @DisplayName("Should adjust stock correctly and prevent negative stock")
    void adjustStockValidation() {
        UUID cafeId = UUID.randomUUID();
        InventoryItem item = new InventoryItem(cafeId, null, "SKU-002", "Milk Carton", "L", new BigDecimal("20.00"), new BigDecimal("5.00"), new BigDecimal("60.00"));

        item.adjustStock(new BigDecimal("-5.00"));
        assertThat(item.getCurrentStock()).isEqualTo(new BigDecimal("15.00"));

        assertThatThrownBy(() -> item.adjustStock(new BigDecimal("-20.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");
    }
}
