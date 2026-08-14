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
@Table(name = "floors")
public class Floor {

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Floor() {
        // JPA standard constructor
    }

    public Floor(UUID id, UUID cafeId, String name, int sortOrder) {
        validateInputs(cafeId, name);
        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.name = name.trim();
        this.sortOrder = sortOrder;
    }

    public Floor(UUID cafeId, String name, int sortOrder) {
        this(UUID.randomUUID(), cafeId, name, sortOrder);
    }

    public Floor(UUID cafeId, String name) {
        this(UUID.randomUUID(), cafeId, name, 0);
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

    public static void validateInputs(UUID cafeId, String name) {
        if (cafeId == null) {
            throw new IllegalArgumentException("Cafe ID must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Floor name must not be empty");
        }
        if (name.trim().length() > 100) {
            throw new IllegalArgumentException("Floor name must not exceed 100 characters");
        }
    }

    public void update(String name, int sortOrder) {
        validateInputs(this.cafeId, name);
        this.name = name.trim();
        this.sortOrder = sortOrder;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCafeId() {
        return cafeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        validateInputs(this.cafeId, name);
        this.name = name.trim();
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
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
        Floor floor = (Floor) o;
        return Objects.equals(id, floor.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
