package com.cafepos.session.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "terminals")
public class Terminal {

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Terminal() {
        // JPA standard constructor
    }

    public Terminal(UUID id, UUID cafeId, String name, String code, boolean active) {
        validateInputs(cafeId, name, code);
        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.name = name.trim();
        this.code = code.trim();
        this.active = active;
    }

    public Terminal(UUID cafeId, String name, String code) {
        this(UUID.randomUUID(), cafeId, name, code, true);
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

    public static void validateInputs(UUID cafeId, String name, String code) {
        if (cafeId == null) {
            throw new IllegalArgumentException("Cafe ID must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Terminal name must not be empty");
        }
        if (name.trim().length() > 100) {
            throw new IllegalArgumentException("Terminal name must not exceed 100 characters");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Terminal code must not be empty");
        }
        if (code.trim().length() > 50) {
            throw new IllegalArgumentException("Terminal code must not exceed 50 characters");
        }
    }

    public void update(String name, String code) {
        validateInputs(this.cafeId, name, code);
        this.name = name.trim();
        this.code = code.trim();
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
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
        validateInputs(this.cafeId, name, this.code);
        this.name = name.trim();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        validateInputs(this.cafeId, this.name, code);
        this.code = code.trim();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
        Terminal terminal = (Terminal) o;
        return Objects.equals(id, terminal.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
