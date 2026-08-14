package com.cafepos.reporting.application;

import java.math.BigDecimal;
import java.util.UUID;

public record TopProductProjection(
        UUID productId,
        String productName,
        String categoryName,
        long totalQuantitySold,
        BigDecimal totalRevenue
) {
}
