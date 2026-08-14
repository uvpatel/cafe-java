package com.cafepos.promotion.api;

import java.util.UUID;

public record PricingPreviewRequest(
        String couponCode,
        String code,
        UUID customerId
) {
    public String getEffectiveCode() {
        if (couponCode != null && !couponCode.isBlank()) {
            return couponCode;
        }
        return code;
    }
}
