package com.cafepos.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryStockMovementTest {

    private final UUID cafeId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();

    @Test
    @DisplayName("Should adjust inventory stock and create stock movement entry")
    void adjustStockAndRecordMovement() {
        InventoryItem item = new InventoryItem(cafeId, null, "COFFEE-01", "Coffee Beans", "KG", new BigDecimal("10.0000"), new BigDecimal("2.0000"), new BigDecimal("15.0000"));

        item.adjustStock(new BigDecimal("-3.5000"));
        assertThat(item.getCurrentStock()).isEqualByComparingTo("6.5000");

        StockMovement movement = new StockMovement(
                cafeId,
                item.getId(),
                StockMovement.MovementType.SALE_DEPLETION,
                new BigDecimal("-3.5000"),
                item.getCurrentStock(),
                "Order sale depletion",
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        assertThat(movement.getId()).isNotNull();
        assertThat(movement.getMovementType()).isEqualTo(StockMovement.MovementType.SALE_DEPLETION);
        assertThat(movement.getQuantityChange()).isEqualByComparingTo("-3.5000");
        assertThat(movement.getResultingStock()).isEqualByComparingTo("6.5000");
    }

    @Test
    @DisplayName("Should throw IllegalStateException when stock adjustment leads to negative inventory")
    void negativeStockAdjustmentThrows() {
        InventoryItem item = new InventoryItem(cafeId, null, "MILK-01", "Whole Milk", "LITER", new BigDecimal("1.0000"), new BigDecimal("0.5000"), new BigDecimal("2.0000"));

        assertThatThrownBy(() -> item.adjustStock(new BigDecimal("-2.0000")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");
    }
}
