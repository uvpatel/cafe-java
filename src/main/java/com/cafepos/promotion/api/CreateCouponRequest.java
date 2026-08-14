package com.cafepos.promotion.api;

import com.cafepos.promotion.domain.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateCouponRequest(
        @NotBlank(message = "Coupon code must not be blank")
        @Size(max = 50, message = "Coupon code must not exceed 50 characters")
        String code,

        @NotNull(message = "Discount type must not be null")
        DiscountType discountType,

        @NotNull(message = "Discount value must not be null")
        @DecimalMin(value = "0.01", message = "Discount value must be positive")
        BigDecimal discountValue,

        @DecimalMin(value = "0.00", message = "Minimum order amount cannot be negative")
        BigDecimal minOrderAmount,

        @Positive(message = "Max redemptions must be greater than zero")
        Integer maxRedemptions,

        Instant startsAt,
        Instant expiresAt
) {
}
