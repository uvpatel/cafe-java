package com.cafepos.promotion.domain;

import com.cafepos.shared.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountValue;

    @Column(name = "min_order_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal minOrderAmount;

    @Column(name = "max_redemptions")
    private Integer maxRedemptions;

    @Column(name = "times_redeemed", nullable = false)
    private int timesRedeemed;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Coupon() {
        // JPA standard constructor
    }

    public Coupon(UUID id, UUID cafeId, String code, DiscountType discountType, BigDecimal discountValue,
                  BigDecimal minOrderAmount, Integer maxRedemptions, Instant startsAt, Instant expiresAt) {
        validateInputs(cafeId, code, discountType, discountValue, minOrderAmount, maxRedemptions, startsAt, expiresAt);
        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.code = normalizeCode(code);
        this.discountType = discountType;
        this.discountValue = discountValue.setScale(4, RoundingMode.HALF_UP);
        this.minOrderAmount = minOrderAmount != null ? minOrderAmount.setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        this.maxRedemptions = maxRedemptions;
        this.timesRedeemed = 0;
        this.active = true;
        this.startsAt = startsAt;
        this.expiresAt = expiresAt;
    }

    public Coupon(UUID cafeId, String code, DiscountType discountType, BigDecimal discountValue) {
        this(UUID.randomUUID(), cafeId, code, discountType, discountValue, BigDecimal.ZERO, null, null, null);
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

    public static String normalizeCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new IllegalArgumentException("Coupon code cannot be null or empty");
        }
        return rawCode.trim().toUpperCase();
    }

    private static void validateInputs(UUID cafeId, String code, DiscountType discountType, BigDecimal discountValue,
                                        BigDecimal minOrderAmount, Integer maxRedemptions, Instant startsAt, Instant expiresAt) {
        if (cafeId == null) {
            throw new IllegalArgumentException("Cafe ID cannot be null");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Coupon code cannot be null or empty");
        }
        if (discountType == null) {
            throw new IllegalArgumentException("Discount type cannot be null");
        }
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Discount value must be positive");
        }
        if (discountType == DiscountType.PERCENTAGE && discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Percentage discount cannot exceed 100%");
        }
        if (minOrderAmount != null && minOrderAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Minimum order amount cannot be negative");
        }
        if (maxRedemptions != null && maxRedemptions <= 0) {
            throw new IllegalArgumentException("Max redemptions must be greater than zero if specified");
        }
        if (startsAt != null && expiresAt != null && startsAt.isAfter(expiresAt)) {
            throw new IllegalArgumentException("Starts at cannot be after expires at");
        }
    }

    public boolean isActiveAt(Instant now) {
        if (!active) {
            return false;
        }
        Instant evalTime = now != null ? now : Instant.now();
        if (startsAt != null && evalTime.isBefore(startsAt)) {
            return false;
        }
        if (expiresAt != null && evalTime.isAfter(expiresAt)) {
            return false;
        }
        return true;
    }

    public boolean hasRemainingRedemptions() {
        return maxRedemptions == null || timesRedeemed < maxRedemptions;
    }

    public boolean meetsMinimumOrder(Money subtotal) {
        if (subtotal == null) {
            return false;
        }
        return subtotal.amount().compareTo(minOrderAmount) >= 0;
    }

    public Money calculateDiscountAmount(Money subtotal) {
        if (subtotal == null || subtotal.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return Money.zero(subtotal != null ? subtotal.currency() : Money.DEFAULT_CURRENCY);
        }
        if (!meetsMinimumOrder(subtotal)) {
            return Money.zero(subtotal.currency());
        }
        if (discountType == DiscountType.FIXED_AMOUNT) {
            Money discount = new Money(discountValue, subtotal.currency());
            return discount.isGreaterThan(subtotal) ? subtotal : discount;
        } else {
            BigDecimal factor = discountValue.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            Money discount = subtotal.multiply(factor);
            return discount.isGreaterThan(subtotal) ? subtotal : discount;
        }
    }

    public void incrementTimesRedeemed() {
        if (maxRedemptions != null && timesRedeemed >= maxRedemptions) {
            throw new IllegalStateException("Cannot redeem coupon: maximum redemption limit reached");
        }
        this.timesRedeemed++;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UUID getCafeId() {
        return cafeId;
    }

    public String getCode() {
        return code;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public Integer getMaxRedemptions() {
        return maxRedemptions;
    }

    public int getTimesRedeemed() {
        return timesRedeemed;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
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
        Coupon coupon = (Coupon) o;
        return Objects.equals(id, coupon.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
