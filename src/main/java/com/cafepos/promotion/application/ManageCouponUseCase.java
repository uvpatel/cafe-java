package com.cafepos.promotion.application;

import com.cafepos.promotion.api.CouponResponse;
import com.cafepos.promotion.api.CreateCouponRequest;

import java.util.List;
import java.util.UUID;

public interface ManageCouponUseCase {

    CouponResponse createCoupon(UUID cafeId, CreateCouponRequest request);

    CouponResponse toggleCouponActiveStatus(UUID cafeId, UUID couponId, boolean active);

    List<CouponResponse> getCouponsForCafe(UUID cafeId);

    CouponResponse getCouponById(UUID cafeId, UUID couponId);
}
