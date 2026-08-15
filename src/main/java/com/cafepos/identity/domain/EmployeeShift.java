package com.cafepos.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "employee_shifts")
public class EmployeeShift {

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "clock_in_at", nullable = false)
    private Instant clockInAt;

    @Column(name = "clock_out_at")
    private Instant clockOutAt;

    @Column(name = "break_start_at")
    private Instant breakStartAt;

    @Column(name = "break_end_at")
    private Instant breakEndAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ShiftStatus status;

    @Column(name = "notes", length = 255)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EmployeeShift() {
        // JPA standard constructor
    }

    public EmployeeShift(UUID id, UUID cafeId, UUID employeeId, String notes) {
        if (cafeId == null) {
            throw new IllegalArgumentException("Cafe ID must not be null");
        }
        if (employeeId == null) {
            throw new IllegalArgumentException("Employee ID must not be null");
        }

        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.employeeId = employeeId;
        this.clockInAt = Instant.now();
        this.status = ShiftStatus.ACTIVE;
        this.notes = notes != null ? notes.trim() : null;
    }

    public EmployeeShift(UUID cafeId, UUID employeeId, String notes) {
        this(UUID.randomUUID(), cafeId, employeeId, notes);
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
        if (this.clockInAt == null) {
            this.clockInAt = now;
        }
        if (this.status == null) {
            this.status = ShiftStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void startBreak() {
        if (this.status != ShiftStatus.ACTIVE) {
            throw new IllegalStateException("Cannot start break on shift with status: " + this.status);
        }
        this.breakStartAt = Instant.now();
        this.status = ShiftStatus.ON_BREAK;
    }

    public void endBreak() {
        if (this.status != ShiftStatus.ON_BREAK) {
            throw new IllegalStateException("Cannot end break when shift status is: " + this.status);
        }
        this.breakEndAt = Instant.now();
        this.status = ShiftStatus.ACTIVE;
    }

    public void clockOut() {
        if (this.status != ShiftStatus.ACTIVE && this.status != ShiftStatus.ON_BREAK) {
            throw new IllegalStateException("Cannot clock out shift with status: " + this.status);
        }
        if (this.status == ShiftStatus.ON_BREAK && this.breakEndAt == null) {
            this.breakEndAt = Instant.now();
        }
        this.clockOutAt = Instant.now();
        this.status = ShiftStatus.COMPLETED;
    }

    public Duration getTotalWorkedDuration() {
        Instant end = this.clockOutAt != null ? this.clockOutAt : Instant.now();
        Duration total = Duration.between(this.clockInAt, end);
        if (this.breakStartAt != null) {
            Instant breakEnd = this.breakEndAt != null ? this.breakEndAt : (this.status == ShiftStatus.ON_BREAK ? Instant.now() : this.breakStartAt);
            Duration breakDur = Duration.between(this.breakStartAt, breakEnd);
            total = total.minus(breakDur);
        }
        return total.isNegative() ? Duration.ZERO : total;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCafeId() {
        return cafeId;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public Instant getClockInAt() {
        return clockInAt;
    }

    public Instant getClockOutAt() {
        return clockOutAt;
    }

    public Instant getBreakStartAt() {
        return breakStartAt;
    }

    public Instant getBreakEndAt() {
        return breakEndAt;
    }

    public ShiftStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
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
        EmployeeShift that = (EmployeeShift) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
