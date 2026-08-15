package com.cafepos.customer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "customer_consents")
public class ConsentRecord {

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "consent_type", nullable = false, length = 50)
    private String consentType;

    @Column(name = "granted", nullable = false)
    private boolean granted;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ConsentRecord() {
        // JPA standard constructor
    }

    public ConsentRecord(UUID id, UUID cafeId, UUID customerId, String consentType, boolean granted, String ipAddress) {
        if (cafeId == null) throw new IllegalArgumentException("cafeId must not be null");
        if (customerId == null) throw new IllegalArgumentException("customerId must not be null");
        if (consentType == null || consentType.isBlank()) throw new IllegalArgumentException("consentType must not be empty");

        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.customerId = customerId;
        this.consentType = consentType.trim().toUpperCase();
        this.granted = granted;
        this.grantedAt = Instant.now();
        this.ipAddress = ipAddress != null ? ipAddress.trim() : null;
    }

    public ConsentRecord(UUID cafeId, UUID customerId, String consentType, boolean granted, String ipAddress) {
        this(UUID.randomUUID(), cafeId, customerId, consentType, granted, ipAddress);
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.grantedAt == null) {
            this.grantedAt = Instant.now();
        }
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    public void revoke() {
        if (!this.granted) {
            throw new IllegalStateException("Consent is already revoked");
        }
        this.granted = false;
        this.revokedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getCafeId() {
        return cafeId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getConsentType() {
        return consentType;
    }

    public boolean isGranted() {
        return granted;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
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
        ConsentRecord that = (ConsentRecord) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
