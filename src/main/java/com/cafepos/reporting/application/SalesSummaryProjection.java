package com.cafepos.reporting.application;

import java.math.BigDecimal;
import java.util.Map;

public record SalesSummaryProjection(
        long totalOrders,
        BigDecimal grossSales,
        BigDecimal discounts,
        BigDecimal tax,
        BigDecimal netRevenue,
        Map<String, BigDecimal> paymentMethodBreakdown,
        BigDecimal cashVariance
) {
}
