package com.cafepos.reporting.api;

import java.math.BigDecimal;
import java.util.UUID;

public record TopProductResponse(
        UUID productId,
        String productName,
        String categoryName,
        long totalQuantitySold,
        BigDecimal totalRevenue
) {
}
