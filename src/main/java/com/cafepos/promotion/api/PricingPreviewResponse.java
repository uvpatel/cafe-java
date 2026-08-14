package com.cafepos.promotion.api;

import java.math.BigDecimal;

public record PricingPreviewResponse(
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String currency,
        String couponCode,
        boolean couponApplied
) {
}
