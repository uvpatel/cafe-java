package com.cafepos.inventory.application;

import com.cafepos.inventory.domain.InventoryItem;
import com.cafepos.inventory.domain.InventoryItemRepository;
import com.cafepos.inventory.domain.StockMovement;
import com.cafepos.inventory.domain.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InventoryApplicationService {

    private final InventoryItemRepository inventoryItemRepository;
    private final StockMovementRepository stockMovementRepository;

    public InventoryApplicationService(InventoryItemRepository inventoryItemRepository,
                                       StockMovementRepository stockMovementRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    public InventoryItem createItem(UUID cafeId, UUID productId, String sku, String itemName, String unitOfMeasure, BigDecimal initialStock, BigDecimal reorderThreshold, BigDecimal costPerUnit) {
        inventoryItemRepository.findByCafeIdAndSku(cafeId, sku.toUpperCase()).ifPresent(item -> {
            throw new IllegalArgumentException("Inventory item with SKU " + sku + " already exists for cafe " + cafeId);
        });

        InventoryItem item = new InventoryItem(cafeId, productId, sku, itemName, unitOfMeasure, initialStock, reorderThreshold, costPerUnit);
        InventoryItem savedItem = inventoryItemRepository.save(item);

        if (initialStock.compareTo(BigDecimal.ZERO) > 0) {
            StockMovement movement = new StockMovement(cafeId, savedItem.getId(), StockMovement.MovementType.MANUAL_ADJUSTMENT, initialStock, initialStock, "Initial stock creation", null, null);
            stockMovementRepository.save(movement);
        }

        return savedItem;
    }

    public InventoryItem adjustStock(UUID cafeId, UUID itemId, StockMovement.MovementType movementType, BigDecimal delta, String reason, UUID referenceOrderId, UUID actorEmployeeId) {
        InventoryItem item = inventoryItemRepository.findByCafeIdAndId(cafeId, itemId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found for ID: " + itemId));

        item.adjustStock(delta);
        InventoryItem updatedItem = inventoryItemRepository.save(item);

        StockMovement movement = new StockMovement(cafeId, item.getId(), movementType, delta, updatedItem.getCurrentStock(), reason, referenceOrderId, actorEmployeeId);
        stockMovementRepository.save(movement);

        return updatedItem;
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> getItems(UUID cafeId) {
        return inventoryItemRepository.findByCafeId(cafeId);
    }

    @Transactional(readOnly = true)
    public List<StockMovement> getMovementsForItem(UUID cafeId, UUID itemId) {
        return stockMovementRepository.findByCafeIdAndInventoryItemIdOrderByCreatedAtDesc(cafeId, itemId);
    }
}
