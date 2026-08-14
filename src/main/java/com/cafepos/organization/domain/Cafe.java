package com.cafepos.organization.domain;

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
@Table(name = "cafes")
public class Cafe {

    @Id
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Cafe() {
        // JPA standard constructor
    }

    public Cafe(UUID id, String name, String slug, String timezone, String currency) {
        String resolvedTimezone = (timezone != null && !timezone.isBlank()) ? timezone.trim() : "UTC";
        String resolvedCurrency = (currency != null && !currency.isBlank()) ? currency.trim().toUpperCase() : "USD";
        validateInputs(name, slug, resolvedTimezone, resolvedCurrency);
        this.id = id != null ? id : UUID.randomUUID();
        this.name = name.trim();
        this.slug = slug.trim().toLowerCase();
        this.timezone = resolvedTimezone;
        this.currency = resolvedCurrency;
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

    private void validateInputs(String name, String slug, String timezone, String currency) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Cafe name must not be empty");
        }
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Cafe slug must not be empty");
        }
        if (timezone == null || timezone.isBlank()) {
            throw new IllegalArgumentException("Cafe timezone must not be empty");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Cafe currency must not be empty");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Cafe name must not be empty");
        }
        this.name = name.trim();
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Cafe slug must not be empty");
        }
        this.slug = slug.trim().toLowerCase();
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            throw new IllegalArgumentException("Cafe timezone must not be empty");
        }
        this.timezone = timezone.trim();
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Cafe currency must not be empty");
        }
        this.currency = currency.trim().toUpperCase();
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
        Cafe cafe = (Cafe) o;
        return Objects.equals(id, cafe.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
