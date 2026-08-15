package com.cafepos.ordering.domain;

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
@Table(name = "order_line_modifiers")
public class OrderLineModifier {

    @Id
    private UUID id;

    @Column(name = "order_line_id", nullable = false)
    private UUID orderLineId;

    @Column(name = "modifier_group_id", nullable = false)
    private UUID modifierGroupId;

    @Column(name = "modifier_option_id", nullable = false)
    private UUID modifierOptionId;

    @Column(name = "option_name_snapshot", nullable = false, length = 100)
    private String optionNameSnapshot;

    @Column(name = "price_delta_snapshot", nullable = false, precision = 19, scale = 4)
    private BigDecimal priceDeltaSnapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OrderLineModifier() {
        // JPA standard constructor
    }

    public OrderLineModifier(UUID id, UUID orderLineId, UUID modifierGroupId, UUID modifierOptionId, String optionNameSnapshot, BigDecimal priceDeltaSnapshot) {
        validateInputs(modifierGroupId, modifierOptionId, optionNameSnapshot, priceDeltaSnapshot);
        this.id = id != null ? id : UUID.randomUUID();
        this.orderLineId = orderLineId;
        this.modifierGroupId = modifierGroupId;
        this.modifierOptionId = modifierOptionId;
        this.optionNameSnapshot = optionNameSnapshot.trim();
        this.priceDeltaSnapshot = priceDeltaSnapshot != null ? priceDeltaSnapshot : BigDecimal.ZERO;
    }

    public OrderLineModifier(UUID orderLineId, UUID modifierGroupId, UUID modifierOptionId, String optionNameSnapshot, BigDecimal priceDeltaSnapshot) {
        this(UUID.randomUUID(), orderLineId, modifierGroupId, modifierOptionId, optionNameSnapshot, priceDeltaSnapshot);
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

    private static void validateInputs(UUID modifierGroupId, UUID modifierOptionId, String optionNameSnapshot, BigDecimal priceDeltaSnapshot) {
        if (modifierGroupId == null) {
            throw new IllegalArgumentException("Modifier group ID must not be null");
        }
        if (modifierOptionId == null) {
            throw new IllegalArgumentException("Modifier option ID must not be null");
        }
        if (optionNameSnapshot == null || optionNameSnapshot.isBlank()) {
            throw new IllegalArgumentException("Option name snapshot must not be empty");
        }
        if (priceDeltaSnapshot != null && priceDeltaSnapshot.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price delta snapshot cannot be negative");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderLineId() {
        return orderLineId;
    }

    void setOrderLineId(UUID orderLineId) {
        this.orderLineId = orderLineId;
    }

    public UUID getModifierGroupId() {
        return modifierGroupId;
    }

    public UUID getModifierOptionId() {
        return modifierOptionId;
    }

    public String getOptionNameSnapshot() {
        return optionNameSnapshot;
    }

    public BigDecimal getPriceDeltaSnapshotAmount() {
        return priceDeltaSnapshot;
    }

    public Money getPriceDeltaSnapshot(Currency currency) {
        return new Money(this.priceDeltaSnapshot, currency);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderLineModifier that = (OrderLineModifier) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
