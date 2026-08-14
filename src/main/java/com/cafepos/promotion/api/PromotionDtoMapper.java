package com.cafepos.promotion.api;

import com.cafepos.promotion.domain.Coupon;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class PromotionDtoMapper {

    public CouponResponse toCouponResponse(Coupon coupon) {
        Objects.requireNonNull(coupon, "coupon must not be null");
        return new CouponResponse(
                coupon.getId(),
                coupon.getCafeId(),
                coupon.getCode(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinOrderAmount(),
                coupon.getMaxRedemptions(),
                coupon.getTimesRedeemed(),
                coupon.isActive(),
                coupon.getStartsAt(),
                coupon.getExpiresAt(),
                coupon.getCreatedAt(),
                coupon.getUpdatedAt()
        );
    }
}
