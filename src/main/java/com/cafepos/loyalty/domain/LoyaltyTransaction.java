package com.cafepos.loyalty.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "loyalty_point_transactions")
public class LoyaltyTransaction {

    public enum TransactionType {
        EARNED,
        REDEEMED,
        EXPIRATION,
        MANUAL_ADJUSTMENT
    }

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "loyalty_account_id", nullable = false)
    private UUID loyaltyAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @Column(name = "points_change", nullable = false)
    private int pointsChange;

    @Column(name = "resulting_points", nullable = false)
    private int resultingPoints;

    @Column(name = "reference_order_id")
    private UUID referenceOrderId;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LoyaltyTransaction() {
        // JPA constructor
    }

    public LoyaltyTransaction(UUID id, UUID cafeId, UUID loyaltyAccountId, TransactionType transactionType, int pointsChange, int resultingPoints, UUID referenceOrderId, String description) {
        if (cafeId == null) throw new IllegalArgumentException("Cafe ID must not be null");
        if (loyaltyAccountId == null) throw new IllegalArgumentException("Loyalty account ID must not be null");
        if (transactionType == null) throw new IllegalArgumentException("Transaction type must not be null");

        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.loyaltyAccountId = loyaltyAccountId;
        this.transactionType = transactionType;
        this.pointsChange = pointsChange;
        this.resultingPoints = resultingPoints;
        this.referenceOrderId = referenceOrderId;
        this.description = description != null ? description.trim() : null;
        this.createdAt = Instant.now();
    }

    public LoyaltyTransaction(UUID cafeId, UUID loyaltyAccountId, TransactionType transactionType, int pointsChange, int resultingPoints, UUID referenceOrderId, String description) {
        this(UUID.randomUUID(), cafeId, loyaltyAccountId, transactionType, pointsChange, resultingPoints, referenceOrderId, description);
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCafeId() { return cafeId; }
    public UUID getLoyaltyAccountId() { return loyaltyAccountId; }
    public TransactionType getTransactionType() { return transactionType; }
    public int getPointsChange() { return pointsChange; }
    public int getResultingPoints() { return resultingPoints; }
    public UUID getReferenceOrderId() { return referenceOrderId; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoyaltyTransaction that = (LoyaltyTransaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
