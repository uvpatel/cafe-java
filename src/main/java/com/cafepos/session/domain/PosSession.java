package com.cafepos.session.domain;

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
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "pos_sessions")
public class PosSession {

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "terminal_id", nullable = false)
    private UUID terminalId;

    @Column(name = "opened_by_employee_id", nullable = false)
    private UUID openedByEmployeeId;

    @Column(name = "closed_by_employee_id")
    private UUID closedByEmployeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PosSessionStatus status;

    @Column(name = "opening_balance_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal openingBalanceAmount;

    @Column(name = "closing_expected_amount", precision = 19, scale = 4)
    private BigDecimal closingExpectedAmount;

    @Column(name = "closing_counted_amount", precision = 19, scale = 4)
    private BigDecimal closingCountedAmount;

    @Column(name = "closing_variance_amount", precision = 19, scale = 4)
    private BigDecimal closingVarianceAmount;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PosSession() {
        // JPA standard constructor
    }

    public PosSession(UUID id, UUID cafeId, UUID terminalId, UUID openedByEmployeeId, BigDecimal openingBalanceAmount) {
        validateOpenInputs(cafeId, terminalId, openedByEmployeeId, openingBalanceAmount);
        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.terminalId = terminalId;
        this.openedByEmployeeId = openedByEmployeeId;
        this.status = PosSessionStatus.OPEN;
        this.openingBalanceAmount = openingBalanceAmount;
        this.openedAt = Instant.now();
    }

    public PosSession(UUID cafeId, UUID terminalId, UUID openedByEmployeeId, BigDecimal openingBalanceAmount) {
        this(UUID.randomUUID(), cafeId, terminalId, openedByEmployeeId, openingBalanceAmount);
    }

    public static PosSession openSession(UUID cafeId, UUID terminalId, UUID openedByEmployeeId, BigDecimal openingBalanceAmount, boolean terminalHasOpenSession) {
        if (terminalHasOpenSession) {
            throw new IllegalStateException("Terminal already has an active OPEN session");
        }
        return new PosSession(cafeId, terminalId, openedByEmployeeId, openingBalanceAmount);
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
        if (this.openedAt == null) {
            this.openedAt = now;
        }
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public static void validateOpenInputs(UUID cafeId, UUID terminalId, UUID openedByEmployeeId, BigDecimal openingBalanceAmount) {
        if (cafeId == null) {
            throw new IllegalArgumentException("Cafe ID must not be null");
        }
        if (terminalId == null) {
            throw new IllegalArgumentException("Terminal ID must not be null");
        }
        if (openedByEmployeeId == null) {
            throw new IllegalArgumentException("Opened-by employee ID must not be null");
        }
        if (openingBalanceAmount == null) {
            throw new IllegalArgumentException("Opening balance amount must not be null");
        }
        if (openingBalanceAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Opening balance amount must not be negative");
        }
    }

    public CashMovement recordCashMovement(UUID employeeId, CashMovementType type, BigDecimal amount, String reason) {
        if (this.status != PosSessionStatus.OPEN) {
            throw new IllegalStateException("Cash movement cannot be recorded on a CLOSED session");
        }
        return new CashMovement(this.cafeId, this.id, employeeId, type, amount, reason);
    }

    public void closeSession(UUID closedByEmployeeId, BigDecimal expectedAmount, BigDecimal countedAmount) {
        if (closedByEmployeeId == null) {
            throw new IllegalArgumentException("Closed-by employee ID must not be null");
        }
        if (expectedAmount == null) {
            throw new IllegalArgumentException("Expected amount must not be null");
        }
        if (countedAmount == null) {
            throw new IllegalArgumentException("Counted amount must not be null");
        }
        if (expectedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Expected amount must not be negative");
        }
        if (countedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Counted amount must not be negative");
        }

        if (this.status == PosSessionStatus.CLOSED) {
            // Idempotent: confirm values match existing closed values or keep original closed state safely
            return;
        }

        this.closedByEmployeeId = closedByEmployeeId;
        this.closingExpectedAmount = expectedAmount;
        this.closingCountedAmount = countedAmount;
        this.closingVarianceAmount = countedAmount.subtract(expectedAmount);
        this.status = PosSessionStatus.CLOSED;
        this.closedAt = Instant.now();
    }

    public boolean isOpen() {
        return this.status == PosSessionStatus.OPEN;
    }

    public boolean isClosed() {
        return this.status == PosSessionStatus.CLOSED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCafeId() {
        return cafeId;
    }

    public UUID getTerminalId() {
        return terminalId;
    }

    public UUID getOpenedByEmployeeId() {
        return openedByEmployeeId;
    }

    public UUID getClosedByEmployeeId() {
        return closedByEmployeeId;
    }

    public PosSessionStatus getStatus() {
        return status;
    }

    public BigDecimal getOpeningBalanceAmount() {
        return openingBalanceAmount;
    }

    public BigDecimal getClosingExpectedAmount() {
        return closingExpectedAmount;
    }

    public BigDecimal getClosingCountedAmount() {
        return closingCountedAmount;
    }

    public BigDecimal getClosingVarianceAmount() {
        return closingVarianceAmount;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
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
        PosSession session = (PosSession) o;
        return Objects.equals(id, session.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
