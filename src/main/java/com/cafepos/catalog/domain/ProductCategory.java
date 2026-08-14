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
import java.util.regex.Pattern;

@Entity
@Table(name = "categories")
public class ProductCategory {

    public static final String DEFAULT_COLOR_HEX = "#6c757d";
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6})$");

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "color_hex", nullable = false, length = 7)
    private String colorHex;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_archived", nullable = false)
    private boolean archived;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProductCategory() {
        // JPA standard constructor
    }

    public ProductCategory(UUID id, UUID cafeId, String name, String colorHex, int sortOrder) {
        String resolvedColor = resolveColorHex(colorHex);
        validateInputs(cafeId, name, resolvedColor);
        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.name = name.trim();
        this.colorHex = resolvedColor;
        this.sortOrder = sortOrder;
        this.archived = false;
    }

    public ProductCategory(UUID cafeId, String name, String colorHex, int sortOrder) {
        this(UUID.randomUUID(), cafeId, name, colorHex, sortOrder);
    }

    public ProductCategory(UUID cafeId, String name) {
        this(UUID.randomUUID(), cafeId, name, DEFAULT_COLOR_HEX, 0);
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

    private static String resolveColorHex(String colorHex) {
        if (colorHex == null || colorHex.isBlank()) {
            return DEFAULT_COLOR_HEX;
        }
        return colorHex.trim();
    }

    public static void validateInputs(UUID cafeId, String name, String colorHex) {
        if (cafeId == null) {
            throw new IllegalArgumentException("Cafe ID must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name must not be empty");
        }
        if (name.trim().length() > 100) {
            throw new IllegalArgumentException("Category name must not exceed 100 characters");
        }
        if (colorHex == null || !HEX_COLOR_PATTERN.matcher(colorHex).matches()) {
            throw new IllegalArgumentException("Color hex must be a valid 7-character hex string (e.g. #6c757d)");
        }
    }

    public void update(String name, String colorHex, int sortOrder) {
        String resolvedColor = resolveColorHex(colorHex);
        validateInputs(this.cafeId, name, resolvedColor);
        this.name = name.trim();
        this.colorHex = resolvedColor;
        this.sortOrder = sortOrder;
    }

    public void archive() {
        this.archived = true;
    }

    public void unarchive() {
        this.archived = false;
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
        validateInputs(this.cafeId, name, this.colorHex);
        this.name = name.trim();
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        String resolvedColor = resolveColorHex(colorHex);
        validateInputs(this.cafeId, this.name, resolvedColor);
        this.colorHex = resolvedColor;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
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
        ProductCategory category = (ProductCategory) o;
        return Objects.equals(id, category.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
