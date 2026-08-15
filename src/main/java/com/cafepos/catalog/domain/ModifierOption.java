package com.cafepos.catalog.domain;

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
@Table(name = "modifier_options")
public class ModifierOption {

    @Id
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "price_delta", nullable = false, precision = 19, scale = 4)
    private BigDecimal priceDelta;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ModifierOption() {
        // JPA standard constructor
    }

    public ModifierOption(UUID id, UUID groupId, String name, BigDecimal priceDelta, boolean isDefault, int sortOrder) {
        validateInputs(groupId, name, priceDelta);
        this.id = id != null ? id : UUID.randomUUID();
        this.groupId = groupId;
        this.name = name.trim();
        this.priceDelta = priceDelta != null ? priceDelta : BigDecimal.ZERO;
        this.isDefault = isDefault;
        this.sortOrder = sortOrder;
    }

    public ModifierOption(UUID groupId, String name, BigDecimal priceDelta, boolean isDefault, int sortOrder) {
        this(UUID.randomUUID(), groupId, name, priceDelta, isDefault, sortOrder);
    }

    public ModifierOption(UUID groupId, String name, BigDecimal priceDelta) {
        this(UUID.randomUUID(), groupId, name, priceDelta, false, 0);
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

    public static void validateInputs(UUID groupId, String name, BigDecimal priceDelta) {
        if (groupId == null) {
            throw new IllegalArgumentException("Modifier group ID must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Modifier option name must not be empty");
        }
        if (priceDelta != null && priceDelta.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Modifier price delta cannot be negative");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPriceDelta() {
        return priceDelta;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public int getSortOrder() {
        return sortOrder;
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
        ModifierOption that = (ModifierOption) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
