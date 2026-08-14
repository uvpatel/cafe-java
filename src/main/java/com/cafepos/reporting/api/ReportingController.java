package com.cafepos.reporting.api;

import com.cafepos.reporting.application.ReportingApplicationService;
import com.cafepos.reporting.application.SalesSummaryProjection;
import com.cafepos.reporting.application.TopProductProjection;
import com.cafepos.shared.tenancy.TenantContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MANAGER')")
public class ReportingController {

    private final ReportingApplicationService reportingApplicationService;

    public ReportingController(ReportingApplicationService reportingApplicationService) {
        this.reportingApplicationService = Objects.requireNonNull(reportingApplicationService, "reportingApplicationService must not be null");
    }

    @GetMapping("/summary")
    public ResponseEntity<SalesSummaryResponse> getSalesSummary(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        SalesSummaryProjection proj = reportingApplicationService.getSalesSummary(cafeId, startDate, endDate);
        SalesSummaryResponse response = new SalesSummaryResponse(
                proj.totalOrders(),
                proj.grossSales(),
                proj.discounts(),
                proj.tax(),
                proj.netRevenue(),
                proj.paymentMethodBreakdown(),
                proj.cashVariance()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<TopProductResponse>> getTopProducts(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        List<TopProductProjection> projs = reportingApplicationService.getTopProducts(cafeId, startDate, endDate, limit);
        List<TopProductResponse> responses = projs.stream()
                .map(p -> new TopProductResponse(p.productId(), p.productName(), p.categoryName(), p.totalQuantitySold(), p.totalRevenue()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
