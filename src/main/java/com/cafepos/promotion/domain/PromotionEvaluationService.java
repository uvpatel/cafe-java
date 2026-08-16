package com.cafepos.promotion.domain;

import com.cafepos.shared.error.BusinessException;
import com.cafepos.shared.money.Money;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

@Service
public class PromotionEvaluationService {

    /**
     * Normalizes a raw coupon code string to trimmed uppercase format.
     */
    public String normalizeCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new BusinessException("INVALID_COUPON_CODE", "Coupon code cannot be empty");
        }
        return rawCode.trim().toUpperCase();
    }

    /**
     * Validates whether a coupon is applicable for the given subtotal and evaluation time.
     * Throws BusinessException if any criteria (active status, time window, redemption limit, min order) fails.
     */
    public void validateCouponApplicable(Coupon coupon, Money subtotal, Instant evaluationTime) {
        Objects.requireNonNull(coupon, "Coupon cannot be null");
        Objects.requireNonNull(subtotal, "Subtotal cannot be null");

        Instant evalTime = evaluationTime != null ? evaluationTime : Instant.now();

        if (!coupon.isActive()) {
            throw new BusinessException("COUPON_INACTIVE", "Coupon is inactive");
        }

        if (coupon.getStartsAt() != null && evalTime.isBefore(coupon.getStartsAt())) {
            throw new BusinessException("COUPON_NOT_STARTED", "Coupon is not active yet");
        }

        if (coupon.getExpiresAt() != null && evalTime.isAfter(coupon.getExpiresAt())) {
            throw new BusinessException("COUPON_EXPIRED", "Coupon has expired");
        }

        if (!coupon.hasRemainingRedemptions()) {
            throw new BusinessException("COUPON_MAX_REDEMPTIONS_REACHED", "Coupon maximum redemption limit reached");
        }

        if (!coupon.meetsMinimumOrder(subtotal)) {
            throw new BusinessException("COUPON_MIN_ORDER_NOT_MET",
                    "Order subtotal " + subtotal.amount() + " does not meet coupon minimum order amount " + coupon.getMinOrderAmount());
        }
    }

    /**
     * Checks if a coupon is applicable without throwing exceptions.
     */
    public boolean isCouponApplicable(Coupon coupon, Money subtotal, Instant evaluationTime) {
        if (coupon == null || subtotal == null) {
            return false;
        }
        try {
            validateCouponApplicable(coupon, subtotal, evaluationTime);
            return true;
        } catch (BusinessException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Calculates the discount amount for a coupon applied to subtotal using Money arithmetic with explicit capping.
     * Capping ensures the discount amount does not exceed subtotal amount and is not negative.
     */
    public Money calculateDiscount(Coupon coupon, Money subtotal) {
        Objects.requireNonNull(coupon, "Coupon cannot be null");
        Objects.requireNonNull(subtotal, "Subtotal cannot be null");

        BigDecimal discountAmount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            BigDecimal percentage = coupon.getDiscountValue();
            BigDecimal factor = percentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            discountAmount = subtotal.amount().multiply(factor).setScale(4, RoundingMode.HALF_UP);
        } else if (coupon.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discountAmount = coupon.getDiscountValue().setScale(4, RoundingMode.HALF_UP);
        } else {
            throw new IllegalStateException("Unsupported discount type: " + coupon.getDiscountType());
        }

        // Cap discount amount between 0 and subtotal amount
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            discountAmount = BigDecimal.ZERO;
        }
        if (discountAmount.compareTo(subtotal.amount()) > 0) {
            discountAmount = subtotal.amount();
        }

        return new Money(discountAmount, subtotal.currency());
    }

    /**
     * Evaluates coupon applicability and calculates the resulting discount amount.
     */
    public Money evaluateDiscount(Coupon coupon, Money subtotal, Instant evaluationTime) {
        validateCouponApplicable(coupon, subtotal, evaluationTime);
        return calculateDiscount(coupon, subtotal);
    }
}
