package com.cafepos.payment.domain;

import com.cafepos.shared.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "refunded_by_employee_id", nullable = false)
    private UUID refundedByEmployeeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Refund() {
        // JPA standard constructor
    }

    public Refund(UUID id, UUID cafeId, UUID paymentId, Money amount, String reason, UUID refundedByEmployeeId) {
        validateInputs(cafeId, paymentId, amount, reason, refundedByEmployeeId);

        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.paymentId = paymentId;
        this.amount = amount.amount();
        this.reason = reason.trim();
        this.refundedByEmployeeId = refundedByEmployeeId;
    }

    public Refund(UUID cafeId, UUID paymentId, Money amount, String reason, UUID refundedByEmployeeId) {
        this(UUID.randomUUID(), cafeId, paymentId, amount, reason, refundedByEmployeeId);
    }

    private static void validateInputs(UUID cafeId, UUID paymentId, Money amount, String reason, UUID refundedByEmployeeId) {
        if (cafeId == null) {
            throw new IllegalArgumentException("Cafe ID must not be null");
        }
        if (paymentId == null) {
            throw new IllegalArgumentException("Payment ID must not be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Refund amount must not be null");
        }
        if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be greater than zero");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Refund reason must not be empty");
        }
        if (refundedByEmployeeId == null) {
            throw new IllegalArgumentException("Refunded by employee ID must not be null");
        }
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    public Money getAmount(Currency currency) {
        return new Money(this.amount, currency);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCafeId() {
        return cafeId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    public UUID getRefundedByEmployeeId() {
        return refundedByEmployeeId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Refund refund = (Refund) o;
        return Objects.equals(id, refund.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
