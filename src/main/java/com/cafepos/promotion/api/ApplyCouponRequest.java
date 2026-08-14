package com.cafepos.promotion.api;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ApplyCouponRequest(
        @NotBlank(message = "Coupon code must not be blank")
        String code,

        UUID customerId
) {
}
