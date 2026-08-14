package com.cafepos.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_accounts")
public class UserAccount {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    @Id
    private UUID id;

    @Column(name = "employee_id", nullable = false, unique = true)
    private UUID employeeId;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserAccount() {
        // JPA standard constructor
    }

    public UserAccount(UUID id, UUID employeeId, String passwordHash) {
        validateInputs(employeeId, passwordHash);
        this.id = id != null ? id : UUID.randomUUID();
        this.employeeId = employeeId;
        this.passwordHash = passwordHash;
        this.failedAttempts = 0;
        this.lockedUntil = null;
        this.lastLoginAt = null;
    }

    public UserAccount(UUID employeeId, String passwordHash) {
        this(UUID.randomUUID(), employeeId, passwordHash);
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

    private void validateInputs(UUID employeeId, String passwordHash) {
        if (employeeId == null) {
            throw new IllegalArgumentException("Employee ID must not be null");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash must not be empty");
        }
    }

    public boolean isLocked(Instant currentTime) {
        if (lockedUntil == null) {
            return false;
        }
        return currentTime.isBefore(lockedUntil);
    }

    public void recordFailedAttempt(Instant currentTime) {
        if (isLocked(currentTime)) {
            return;
        }
        this.failedAttempts++;
        if (this.failedAttempts >= MAX_FAILED_ATTEMPTS) {
            this.lockedUntil = currentTime.plus(LOCK_DURATION);
        }
    }

    public void recordSuccessfulLogin(Instant currentTime) {
        this.failedAttempts = 0;
        this.lockedUntil = null;
        this.lastLoginAt = currentTime;
    }

    public void unlock() {
        this.failedAttempts = 0;
        this.lockedUntil = null;
    }

    public void updatePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new IllegalArgumentException("New password hash must not be empty");
        }
        this.passwordHash = newPasswordHash;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
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
        UserAccount that = (UserAccount) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
