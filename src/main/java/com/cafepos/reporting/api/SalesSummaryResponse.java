package com.cafepos.reporting.api;

import java.math.BigDecimal;
import java.util.Map;

public record SalesSummaryResponse(
        long totalOrders,
        BigDecimal grossSales,
        BigDecimal discounts,
        BigDecimal tax,
        BigDecimal netRevenue,
        Map<String, BigDecimal> paymentMethodBreakdown,
        BigDecimal cashVariance
) {
}
