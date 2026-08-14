package com.cafepos.catalog.domain;

import com.cafepos.shared.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "sku", length = 50)
    private String sku;

    @Column(name = "price_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal priceAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private UnitOfMeasure unitOfMeasure;

    @Column(name = "is_kitchen_routed", nullable = false)
    private boolean kitchenRouted;

    @Column(name = "is_archived", nullable = false)
    private boolean archived;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Product() {
        // JPA standard constructor
    }

    public Product(UUID id, UUID cafeId, UUID categoryId, String name, String sku, Money price, UnitOfMeasure unitOfMeasure, boolean kitchenRouted) {
        validateInputs(cafeId, categoryId, name, price);
        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.categoryId = categoryId;
        this.name = name.trim();
        this.sku = (sku != null && !sku.isBlank()) ? sku.trim() : null;
        this.priceAmount = price.amount();
        this.currency = price.currency().getCurrencyCode();
        this.unitOfMeasure = unitOfMeasure != null ? unitOfMeasure : UnitOfMeasure.UNIT;
        this.kitchenRouted = kitchenRouted;
        this.archived = false;
    }

    public Product(UUID cafeId, UUID categoryId, String name, Money price) {
        this(UUID.randomUUID(), cafeId, categoryId, name, null, price, UnitOfMeasure.UNIT, true);
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

    public static void validateInputs(UUID cafeId, UUID categoryId, String name, Money price) {
        if (cafeId == null) {
            throw new IllegalArgumentException("Cafe ID must not be null");
        }
        if (categoryId == null) {
            throw new IllegalArgumentException("Category ID must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name must not be empty");
        }
        if (name.trim().length() > 100) {
            throw new IllegalArgumentException("Product name must not exceed 100 characters");
        }
        validateMoneyPrice(price);
    }

    public static void validateMoneyPrice(Money price) {
        if (price == null) {
            throw new IllegalArgumentException("Price must not be null");
        }
        if (price.amount() == null) {
            throw new IllegalArgumentException("Price amount must not be null");
        }
        if (price.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price amount cannot be negative");
        }
        if (price.currency() == null) {
            throw new IllegalArgumentException("Price currency must not be null");
        }
    }

    public Money getPrice() {
        return new Money(this.priceAmount, Currency.getInstance(this.currency));
    }

    public void setPrice(Money price) {
        validateMoneyPrice(price);
        this.priceAmount = price.amount();
        this.currency = price.currency().getCurrencyCode();
    }

    public void updateDetails(String name, String sku, UUID categoryId, Money price, UnitOfMeasure unitOfMeasure, boolean kitchenRouted) {
        validateInputs(this.cafeId, categoryId, name, price);
        this.name = name.trim();
        this.sku = (sku != null && !sku.isBlank()) ? sku.trim() : null;
        this.categoryId = categoryId;
        setPrice(price);
        this.unitOfMeasure = unitOfMeasure != null ? unitOfMeasure : UnitOfMeasure.UNIT;
        this.kitchenRouted = kitchenRouted;
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

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Category ID must not be null");
        }
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name must not be empty");
        }
        if (name.trim().length() > 100) {
            throw new IllegalArgumentException("Product name must not exceed 100 characters");
        }
        this.name = name.trim();
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = (sku != null && !sku.isBlank()) ? sku.trim() : null;
    }

    public UnitOfMeasure getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(UnitOfMeasure unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure != null ? unitOfMeasure : UnitOfMeasure.UNIT;
    }

    public boolean isKitchenRouted() {
        return kitchenRouted;
    }

    public void setKitchenRouted(boolean kitchenRouted) {
        this.kitchenRouted = kitchenRouted;
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
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
