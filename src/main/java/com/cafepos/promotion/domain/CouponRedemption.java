package com.cafepos.promotion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "coupon_redemptions")
public class CouponRedemption {

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "coupon_id", nullable = false)
    private UUID couponId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "redeemed_at", nullable = false, updatable = false)
    private Instant redeemedAt;

    protected CouponRedemption() {
        // JPA standard constructor
    }

    public CouponRedemption(UUID id, UUID cafeId, UUID couponId, UUID orderId, UUID customerId, Instant redeemedAt) {
        if (cafeId == null) {
            throw new IllegalArgumentException("Cafe ID cannot be null");
        }
        if (couponId == null) {
            throw new IllegalArgumentException("Coupon ID cannot be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.couponId = couponId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.redeemedAt = redeemedAt != null ? redeemedAt : Instant.now();
    }

    public CouponRedemption(UUID cafeId, UUID couponId, UUID orderId, UUID customerId) {
        this(UUID.randomUUID(), cafeId, couponId, orderId, customerId, Instant.now());
    }

    public CouponRedemption(UUID cafeId, UUID couponId, UUID orderId) {
        this(UUID.randomUUID(), cafeId, couponId, orderId, null, Instant.now());
    }

    @PrePersist
    protected void onCreate() {
        if (this.redeemedAt == null) {
            this.redeemedAt = Instant.now();
        }
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getCafeId() {
        return cafeId;
    }

    public UUID getCouponId() {
        return couponId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public Instant getRedeemedAt() {
        return redeemedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CouponRedemption that = (CouponRedemption) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
