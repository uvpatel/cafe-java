package com.cafepos.inventory.application;

import com.cafepos.inventory.domain.InventoryItem;
import com.cafepos.inventory.domain.InventoryItemRepository;
import com.cafepos.inventory.domain.StockMovement;
import com.cafepos.inventory.domain.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryApplicationServiceTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    private InventoryApplicationService inventoryApplicationService;

    private final UUID cafeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        inventoryApplicationService = new InventoryApplicationService(inventoryItemRepository, stockMovementRepository);
    }

    @Test
    @DisplayName("Should create item and initial stock movement")
    void createItemSuccess() {
        when(inventoryItemRepository.findByCafeIdAndSku(cafeId, "COFFEE-01")).thenReturn(Optional.empty());
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryItem item = inventoryApplicationService.createItem(
                cafeId, null, "COFFEE-01", "Coffee Beans", "KG",
                new BigDecimal("10.0000"), new BigDecimal("2.0000"), new BigDecimal("15.0000")
        );

        assertThat(item).isNotNull();
        assertThat(item.getSku()).isEqualTo("COFFEE-01");
        verify(inventoryItemRepository).save(any(InventoryItem.class));
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    @Test
    @DisplayName("Should adjust stock and record movement entry")
    void adjustStockSuccess() {
        UUID itemId = UUID.randomUUID();
        InventoryItem existingItem = new InventoryItem(itemId, cafeId, null, "MILK-01", "Whole Milk", "LITER", new BigDecimal("5.0000"), new BigDecimal("1.0000"), new BigDecimal("2.0000"));

        when(inventoryItemRepository.findByCafeIdAndId(cafeId, itemId)).thenReturn(Optional.of(existingItem));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryItem updated = inventoryApplicationService.adjustStock(
                cafeId, itemId, StockMovement.MovementType.MANUAL_ADJUSTMENT, new BigDecimal("3.0000"), "Restock", null, UUID.randomUUID()
        );

        assertThat(updated.getCurrentStock()).isEqualByComparingTo("8.0000");
        verify(stockMovementRepository).save(any(StockMovement.class));
    }
}
