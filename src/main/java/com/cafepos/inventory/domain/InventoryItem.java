package com.cafepos.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "sku", nullable = false, length = 50)
    private String sku;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Column(name = "unit_of_measure", nullable = false, length = 30)
    private String unitOfMeasure;

    @Column(name = "current_stock", nullable = false, precision = 12, scale = 4)
    private BigDecimal currentStock;

    @Column(name = "reorder_threshold", nullable = false, precision = 12, scale = 4)
    private BigDecimal reorderThreshold;

    @Column(name = "cost_per_unit", nullable = false, precision = 12, scale = 4)
    private BigDecimal costPerUnit;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InventoryItem() {
        // JPA constructor
    }

    public InventoryItem(UUID id, UUID cafeId, UUID productId, String sku, String itemName, String unitOfMeasure, BigDecimal currentStock, BigDecimal reorderThreshold, BigDecimal costPerUnit) {
        validateInputs(cafeId, sku, itemName, unitOfMeasure, currentStock, reorderThreshold, costPerUnit);
        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.productId = productId;
        this.sku = sku.trim().toUpperCase();
        this.itemName = itemName.trim();
        this.unitOfMeasure = unitOfMeasure.trim();
        this.currentStock = currentStock;
        this.reorderThreshold = reorderThreshold;
        this.costPerUnit = costPerUnit;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public InventoryItem(UUID cafeId, UUID productId, String sku, String itemName, String unitOfMeasure, BigDecimal currentStock, BigDecimal reorderThreshold, BigDecimal costPerUnit) {
        this(UUID.randomUUID(), cafeId, productId, sku, itemName, unitOfMeasure, currentStock, reorderThreshold, costPerUnit);
    }

    private static void validateInputs(UUID cafeId, String sku, String itemName, String unitOfMeasure, BigDecimal currentStock, BigDecimal reorderThreshold, BigDecimal costPerUnit) {
        if (cafeId == null) throw new IllegalArgumentException("Cafe ID must not be null");
        if (sku == null || sku.isBlank()) throw new IllegalArgumentException("SKU must not be empty");
        if (itemName == null || itemName.isBlank()) throw new IllegalArgumentException("Item name must not be empty");
        if (unitOfMeasure == null || unitOfMeasure.isBlank()) throw new IllegalArgumentException("Unit of measure must not be empty");
        if (currentStock == null || currentStock.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Current stock must be non-negative");
        if (reorderThreshold == null || reorderThreshold.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Reorder threshold must be non-negative");
        if (costPerUnit == null || costPerUnit.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Cost per unit must be non-negative");
    }

    public void adjustStock(BigDecimal delta) {
        if (delta == null) throw new IllegalArgumentException("Delta must not be null");
        BigDecimal newStock = this.currentStock.add(delta);
        if (newStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Insufficient stock for item SKU: " + sku + ". Available: " + currentStock + ", requested change: " + delta);
        }
        this.currentStock = newStock;
        this.updatedAt = Instant.now();
    }

    public boolean isLowStock() {
        return this.currentStock.compareTo(this.reorderThreshold) <= 0;
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCafeId() { return cafeId; }
    public UUID getProductId() { return productId; }
    public String getSku() { return sku; }
    public String getItemName() { return itemName; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public BigDecimal getCurrentStock() { return currentStock; }
    public BigDecimal getReorderThreshold() { return reorderThreshold; }
    public BigDecimal getCostPerUnit() { return costPerUnit; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryItem item = (InventoryItem) o;
        return Objects.equals(id, item.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
