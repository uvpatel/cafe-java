package com.cafepos.session.domain;

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
@Table(name = "cash_movements")
public class CashMovement {

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CashMovementType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CashMovement() {
        // JPA standard constructor
    }

    public CashMovement(UUID id, UUID cafeId, UUID sessionId, UUID employeeId, CashMovementType type, BigDecimal amount, String reason) {
        validateInputs(cafeId, sessionId, employeeId, type, amount, reason);
        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.sessionId = sessionId;
        this.employeeId = employeeId;
        this.type = type;
        this.amount = amount;
        this.reason = reason.trim();
    }

    public CashMovement(UUID cafeId, UUID sessionId, UUID employeeId, CashMovementType type, BigDecimal amount, String reason) {
        this(UUID.randomUUID(), cafeId, sessionId, employeeId, type, amount, reason);
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

    public static void validateInputs(UUID cafeId, UUID sessionId, UUID employeeId, CashMovementType type, BigDecimal amount, String reason) {
        if (cafeId == null) {
            throw new IllegalArgumentException("Cafe ID must not be null");
        }
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID must not be null");
        }
        if (employeeId == null) {
            throw new IllegalArgumentException("Employee ID must not be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Cash movement type must not be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason must not be empty");
        }
        if (reason.trim().length() > 255) {
            throw new IllegalArgumentException("Reason must not exceed 255 characters");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getCafeId() {
        return cafeId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public CashMovementType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CashMovement movement = (CashMovement) o;
        return Objects.equals(id, movement.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
