package com.cafepos.payment.domain;

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
@Table(name = "order_split_payments")
public class SplitPayment {

    public enum SplitStatus {
        PENDING,
        COMPLETED,
        REFUNDED
    }

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "split_index", nullable = false)
    private int splitIndex;

    @Column(name = "payment_method_id", nullable = false)
    private UUID paymentMethodId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal amount;

    @Column(name = "tip_amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal tipAmount;

    @Column(name = "payer_name", length = 100)
    private String payerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SplitStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SplitPayment() {
        // JPA constructor
    }

    public SplitPayment(UUID id, UUID cafeId, UUID orderId, int splitIndex, UUID paymentMethodId, BigDecimal amount, BigDecimal tipAmount, String payerName) {
        if (cafeId == null) throw new IllegalArgumentException("Cafe ID must not be null");
        if (orderId == null) throw new IllegalArgumentException("Order ID must not be null");
        if (paymentMethodId == null) throw new IllegalArgumentException("Payment method ID must not be null");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (tipAmount == null || tipAmount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Tip amount must not be negative");

        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.orderId = orderId;
        this.splitIndex = splitIndex;
        this.paymentMethodId = paymentMethodId;
        this.amount = amount;
        this.tipAmount = tipAmount;
        this.payerName = payerName != null ? payerName.trim() : null;
        this.status = SplitStatus.COMPLETED;
        this.createdAt = Instant.now();
    }

    public SplitPayment(UUID cafeId, UUID orderId, int splitIndex, UUID paymentMethodId, BigDecimal amount, BigDecimal tipAmount, String payerName) {
        this(UUID.randomUUID(), cafeId, orderId, splitIndex, paymentMethodId, amount, tipAmount, payerName);
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCafeId() { return cafeId; }
    public UUID getOrderId() { return orderId; }
    public int getSplitIndex() { return splitIndex; }
    public UUID getPaymentMethodId() { return paymentMethodId; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getTipAmount() { return tipAmount; }
    public String getPayerName() { return payerName; }
    public SplitStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SplitPayment that = (SplitPayment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
