package com.cafepos.promotion.api;

import com.cafepos.promotion.domain.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CouponResponse(
        UUID id,
        UUID cafeId,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minOrderAmount,
        Integer maxRedemptions,
        int timesRedeemed,
        boolean active,
        Instant startsAt,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
}
