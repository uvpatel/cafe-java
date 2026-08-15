package com.cafepos.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "inventory_stock_movements")
public class StockMovement {

    public enum MovementType {
        PURCHASE_RECEIPT,
        SALE_DEPLETION,
        MANUAL_ADJUSTMENT,
        WASTAGE_SPOILAGE,
        RETURN_RESTOCK
    }

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "inventory_item_id", nullable = false)
    private UUID inventoryItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 50)
    private MovementType movementType;

    @Column(name = "quantity_change", nullable = false, precision = 12, scale = 4)
    private BigDecimal quantityChange;

    @Column(name = "resulting_stock", nullable = false, precision = 12, scale = 4)
    private BigDecimal resultingStock;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "reference_order_id")
    private UUID referenceOrderId;

    @Column(name = "actor_employee_id")
    private UUID actorEmployeeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StockMovement() {
        // JPA constructor
    }

    public StockMovement(UUID id, UUID cafeId, UUID inventoryItemId, MovementType movementType, BigDecimal quantityChange, BigDecimal resultingStock, String reason, UUID referenceOrderId, UUID actorEmployeeId) {
        if (cafeId == null) throw new IllegalArgumentException("Cafe ID must not be null");
        if (inventoryItemId == null) throw new IllegalArgumentException("Inventory item ID must not be null");
        if (movementType == null) throw new IllegalArgumentException("Movement type must not be null");
        if (quantityChange == null) throw new IllegalArgumentException("Quantity change must not be null");
        if (resultingStock == null) throw new IllegalArgumentException("Resulting stock must not be null");

        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.inventoryItemId = inventoryItemId;
        this.movementType = movementType;
        this.quantityChange = quantityChange;
        this.resultingStock = resultingStock;
        this.reason = reason != null ? reason.trim() : null;
        this.referenceOrderId = referenceOrderId;
        this.actorEmployeeId = actorEmployeeId;
        this.createdAt = Instant.now();
    }

    public StockMovement(UUID cafeId, UUID inventoryItemId, MovementType movementType, BigDecimal quantityChange, BigDecimal resultingStock, String reason, UUID referenceOrderId, UUID actorEmployeeId) {
        this(UUID.randomUUID(), cafeId, inventoryItemId, movementType, quantityChange, resultingStock, reason, referenceOrderId, actorEmployeeId);
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCafeId() { return cafeId; }
    public UUID getInventoryItemId() { return inventoryItemId; }
    public MovementType getMovementType() { return movementType; }
    public BigDecimal getQuantityChange() { return quantityChange; }
    public BigDecimal getResultingStock() { return resultingStock; }
    public String getReason() { return reason; }
    public UUID getReferenceOrderId() { return referenceOrderId; }
    public UUID getActorEmployeeId() { return actorEmployeeId; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockMovement movement = (StockMovement) o;
        return Objects.equals(id, movement.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
