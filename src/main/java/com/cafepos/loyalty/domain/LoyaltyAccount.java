package com.cafepos.loyalty.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "customer_loyalty_accounts")
public class LoyaltyAccount {

    public enum TierLevel {
        BRONZE,
        SILVER,
        GOLD,
        PLATINUM
    }

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "current_points", nullable = false)
    private int currentPoints;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier_level", nullable = false, length = 30)
    private TierLevel tierLevel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LoyaltyAccount() {
        // JPA constructor
    }

    public LoyaltyAccount(UUID id, UUID cafeId, UUID customerId, int currentPoints, TierLevel tierLevel) {
        if (cafeId == null) throw new IllegalArgumentException("Cafe ID must not be null");
        if (customerId == null) throw new IllegalArgumentException("Customer ID must not be null");
        if (currentPoints < 0) throw new IllegalArgumentException("Points cannot be negative");

        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.customerId = customerId;
        this.currentPoints = currentPoints;
        this.tierLevel = tierLevel != null ? tierLevel : TierLevel.BRONZE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        recalculateTier();
    }

    public LoyaltyAccount(UUID cafeId, UUID customerId) {
        this(UUID.randomUUID(), cafeId, customerId, 0, TierLevel.BRONZE);
    }

    public void addPoints(int points) {
        if (points <= 0) throw new IllegalArgumentException("Earned points must be positive");
        this.currentPoints += points;
        recalculateTier();
        this.updatedAt = Instant.now();
    }

    public void redeemPoints(int points) {
        if (points <= 0) throw new IllegalArgumentException("Redeemed points must be positive");
        if (this.currentPoints < points) {
            throw new IllegalStateException("Insufficient loyalty points. Available: " + currentPoints + ", requested: " + points);
        }
        this.currentPoints -= points;
        recalculateTier();
        this.updatedAt = Instant.now();
    }

    private void recalculateTier() {
        if (this.currentPoints >= 5000) {
            this.tierLevel = TierLevel.PLATINUM;
        } else if (this.currentPoints >= 2000) {
            this.tierLevel = TierLevel.GOLD;
        } else if (this.currentPoints >= 500) {
            this.tierLevel = TierLevel.SILVER;
        } else {
            this.tierLevel = TierLevel.BRONZE;
        }
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
    public UUID getCustomerId() { return customerId; }
    public int getCurrentPoints() { return currentPoints; }
    public TierLevel getTierLevel() { return tierLevel; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoyaltyAccount account = (LoyaltyAccount) o;
        return Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
