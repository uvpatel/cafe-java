package com.cafepos.floor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "dining_tables")
public class DiningTable {

    public static final int DEFAULT_SEATING_CAPACITY = 4;

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "floor_id", nullable = false)
    private UUID floorId;

    @Column(name = "table_number", nullable = false, length = 30)
    private String tableNumber;

    @Column(name = "seating_capacity", nullable = false)
    private int seatingCapacity;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DiningTable() {
        // JPA standard constructor
    }

    public DiningTable(UUID id, UUID cafeId, UUID floorId, String tableNumber, int seatingCapacity, boolean active) {
        validateInputs(cafeId, floorId, tableNumber, seatingCapacity);
        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.floorId = floorId;
        this.tableNumber = tableNumber.trim();
        this.seatingCapacity = seatingCapacity;
        this.active = active;
    }

    public DiningTable(UUID cafeId, UUID floorId, String tableNumber, int seatingCapacity) {
        this(UUID.randomUUID(), cafeId, floorId, tableNumber, seatingCapacity, true);
    }

    public DiningTable(UUID cafeId, UUID floorId, String tableNumber) {
        this(UUID.randomUUID(), cafeId, floorId, tableNumber, DEFAULT_SEATING_CAPACITY, true);
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public static void validateInputs(UUID cafeId, UUID floorId, String tableNumber, int seatingCapacity) {
        if (cafeId == null) {
            throw new IllegalArgumentException("Cafe ID must not be null");
        }
        if (floorId == null) {
            throw new IllegalArgumentException("Floor ID must not be null");
        }
        if (tableNumber == null || tableNumber.isBlank()) {
            throw new IllegalArgumentException("Table number must not be empty");
        }
        if (tableNumber.trim().length() > 30) {
            throw new IllegalArgumentException("Table number must not exceed 30 characters");
        }
        if (seatingCapacity <= 0) {
            throw new IllegalArgumentException("Seating capacity must be greater than 0");
        }
    }

    public void update(UUID floorId, String tableNumber, int seatingCapacity) {
        validateInputs(this.cafeId, floorId, tableNumber, seatingCapacity);
        this.floorId = floorId;
        this.tableNumber = tableNumber.trim();
        this.seatingCapacity = seatingCapacity;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCafeId() {
        return cafeId;
    }

    public UUID getFloorId() {
        return floorId;
    }

    public void setFloorId(UUID floorId) {
        validateInputs(this.cafeId, floorId, this.tableNumber, this.seatingCapacity);
        this.floorId = floorId;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        validateInputs(this.cafeId, this.floorId, tableNumber, this.seatingCapacity);
        this.tableNumber = tableNumber.trim();
    }

    public int getSeatingCapacity() {
        return seatingCapacity;
    }

    public void setSeatingCapacity(int seatingCapacity) {
        validateInputs(this.cafeId, this.floorId, this.tableNumber, seatingCapacity);
        this.seatingCapacity = seatingCapacity;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiningTable that = (DiningTable) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
