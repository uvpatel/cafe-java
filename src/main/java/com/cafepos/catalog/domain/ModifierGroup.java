package com.cafepos.catalog.domain;

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
@Table(name = "modifier_groups")
public class ModifierGroup {

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "min_selection", nullable = false)
    private int minSelection;

    @Column(name = "max_selection", nullable = false)
    private int maxSelection;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ModifierGroup() {
        // JPA standard constructor
    }

    public ModifierGroup(UUID id, UUID cafeId, String name, int minSelection, int maxSelection) {
        validateInputs(cafeId, name, minSelection, maxSelection);
        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.name = name.trim();
        this.minSelection = minSelection;
        this.maxSelection = maxSelection;
    }

    public ModifierGroup(UUID cafeId, String name, int minSelection, int maxSelection) {
        this(UUID.randomUUID(), cafeId, name, minSelection, maxSelection);
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

    public static void validateInputs(UUID cafeId, String name, int minSelection, int maxSelection) {
        if (cafeId == null) {
            throw new IllegalArgumentException("Cafe ID must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Modifier group name must not be empty");
        }
        if (minSelection < 0) {
            throw new IllegalArgumentException("Minimum selection cannot be negative");
        }
        if (maxSelection < minSelection) {
            throw new IllegalArgumentException("Maximum selection cannot be less than minimum selection");
        }
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

    public int getMinSelection() {
        return minSelection;
    }

    public int getMaxSelection() {
        return maxSelection;
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
        ModifierGroup that = (ModifierGroup) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
