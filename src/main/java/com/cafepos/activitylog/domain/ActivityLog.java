package com.cafepos.activitylog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "activity_logs")
public class ActivityLog {

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "actor_employee_id")
    private UUID actorEmployeeId;

    @Column(name = "actor_email", nullable = false, length = 255)
    private String actorEmail;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 50)
    private String entityId;

    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ActivityLog() {
        // JPA standard constructor
    }

    public ActivityLog(UUID id,
                       UUID cafeId,
                       UUID actorEmployeeId,
                       String actorEmail,
                       String action,
                       String entityType,
                       String entityId,
                       String detailsJson,
                       String ipAddress) {
        validateInputs(cafeId, actorEmail, action, entityType, entityId);
        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.actorEmployeeId = actorEmployeeId;
        this.actorEmail = actorEmail.trim();
        this.action = action.trim();
        this.entityType = entityType.trim();
        this.entityId = entityId.trim();
        this.detailsJson = detailsJson;
        this.ipAddress = ipAddress != null ? ipAddress.trim() : null;
        this.createdAt = Instant.now();
    }

    public ActivityLog(UUID cafeId,
                       UUID actorEmployeeId,
                       String actorEmail,
                       String action,
                       String entityType,
                       String entityId,
                       String detailsJson,
                       String ipAddress) {
        this(UUID.randomUUID(), cafeId, actorEmployeeId, actorEmail, action, entityType, entityId, detailsJson, ipAddress);
    }

    private static void validateInputs(UUID cafeId, String actorEmail, String action, String entityType, String entityId) {
        if (cafeId == null) {
            throw new IllegalArgumentException("Cafe ID must not be null");
        }
        if (actorEmail == null || actorEmail.isBlank()) {
            throw new IllegalArgumentException("Actor email must not be empty");
        }
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Action must not be empty");
        }
        if (entityType == null || entityType.isBlank()) {
            throw new IllegalArgumentException("Entity type must not be empty");
        }
        if (entityId == null || entityId.isBlank()) {
            throw new IllegalArgumentException("Entity ID must not be empty");
        }
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getCafeId() {
        return cafeId;
    }

    public UUID getActorEmployeeId() {
        return actorEmployeeId;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActivityLog that = (ActivityLog) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
