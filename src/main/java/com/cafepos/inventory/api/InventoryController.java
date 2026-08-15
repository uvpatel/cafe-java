package com.cafepos.inventory.api;

import com.cafepos.inventory.application.InventoryApplicationService;
import com.cafepos.inventory.domain.InventoryItem;
import com.cafepos.inventory.domain.StockMovement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cafes/{cafeId}/inventory")
public class InventoryController {

    private final InventoryApplicationService inventoryApplicationService;

    public InventoryController(InventoryApplicationService inventoryApplicationService) {
        this.inventoryApplicationService = inventoryApplicationService;
    }

    public record CreateInventoryItemRequest(
            UUID productId,
            @NotBlank String sku,
            @NotBlank String itemName,
            @NotBlank String unitOfMeasure,
            @NotNull @DecimalMin("0.0") BigDecimal initialStock,
            @NotNull @DecimalMin("0.0") BigDecimal reorderThreshold,
            @NotNull @DecimalMin("0.0") BigDecimal costPerUnit
    ) {}

    public record StockAdjustmentRequest(
            @NotNull StockMovement.MovementType movementType,
            @NotNull BigDecimal delta,
            String reason,
            UUID referenceOrderId,
            UUID actorEmployeeId
    ) {}

    @PostMapping("/items")
    public ResponseEntity<InventoryItem> createItem(@PathVariable UUID cafeId, @Valid @RequestBody CreateInventoryItemRequest request) {
        InventoryItem item = inventoryApplicationService.createItem(
                cafeId, request.productId(), request.sku(), request.itemName(),
                request.unitOfMeasure(), request.initialStock(), request.reorderThreshold(), request.costPerUnit()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @GetMapping("/items")
    public ResponseEntity<List<InventoryItem>> getItems(@PathVariable UUID cafeId) {
        return ResponseEntity.ok(inventoryApplicationService.getItems(cafeId));
    }

    @PostMapping("/items/{itemId}/adjust")
    public ResponseEntity<InventoryItem> adjustStock(@PathVariable UUID cafeId, @PathVariable UUID itemId, @Valid @RequestBody StockAdjustmentRequest request) {
        InventoryItem item = inventoryApplicationService.adjustStock(
                cafeId, itemId, request.movementType(), request.delta(),
                request.reason(), request.referenceOrderId(), request.actorEmployeeId()
        );
        return ResponseEntity.ok(item);
    }

    @GetMapping("/items/{itemId}/movements")
    public ResponseEntity<List<StockMovement>> getMovements(@PathVariable UUID cafeId, @PathVariable UUID itemId) {
        return ResponseEntity.ok(inventoryApplicationService.getMovementsForItem(cafeId, itemId));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryItem>> getLowStockItems(@PathVariable UUID cafeId) {
        return ResponseEntity.ok(inventoryApplicationService.getLowStockItems(cafeId));
    }
}
