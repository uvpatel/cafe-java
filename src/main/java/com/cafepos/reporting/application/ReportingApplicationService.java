package com.cafepos.reporting.application;

import com.cafepos.organization.domain.Cafe;
import com.cafepos.organization.domain.CafeRepository;
import com.cafepos.ordering.domain.Order;
import com.cafepos.ordering.domain.OrderRepository;
import com.cafepos.ordering.domain.OrderStatus;
import com.cafepos.payment.domain.Payment;
import com.cafepos.payment.domain.PaymentRepository;
import com.cafepos.payment.domain.PaymentStatus;
import com.cafepos.session.domain.PosSession;
import com.cafepos.session.domain.PosSessionRepository;
import com.cafepos.reporting.domain.ExportJob;
import com.cafepos.reporting.domain.ExportJobRepository;
import com.cafepos.reporting.domain.ExportType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportingApplicationService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PosSessionRepository posSessionRepository;
    private final CafeRepository cafeRepository;
    private final ExportJobRepository exportJobRepository;
    private final ExportJobRunner exportJobRunner;

    public ReportingApplicationService(OrderRepository orderRepository,
                                       PaymentRepository paymentRepository,
                                       PosSessionRepository posSessionRepository,
                                       CafeRepository cafeRepository,
                                       ExportJobRepository exportJobRepository,
                                       ExportJobRunner exportJobRunner) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.posSessionRepository = posSessionRepository;
        this.cafeRepository = cafeRepository;
        this.exportJobRepository = exportJobRepository;
        this.exportJobRunner = exportJobRunner;
    }

    public SalesSummaryProjection getSalesSummary(UUID cafeId, Instant startDate, Instant endDate) {
        ZoneId cafeZone = getCafeZoneId(cafeId);

        List<Order> orders = orderRepository.findByCafeId(cafeId);
        List<Order> filteredOrders = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID)
                .filter(o -> isWithinTimezoneRange(o.getCreatedAt(), startDate, endDate, cafeZone))
                .collect(Collectors.toList());

        long totalOrders = filteredOrders.size();

        BigDecimal grossSales = filteredOrders.stream()
                .map(o -> o.getSubtotal().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discounts = filteredOrders.stream()
                .map(o -> o.getDiscount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tax = filteredOrders.stream()
                .map(o -> o.getTax().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netRevenue = filteredOrders.stream()
                .map(o -> o.getTotal().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Payment method breakdown
        List<Payment> payments = paymentRepository.findByCafeId(cafeId);
        Map<String, BigDecimal> paymentMethodBreakdown = new HashMap<>();

        List<UUID> paidOrderIds = filteredOrders.stream().map(Order::getId).collect(Collectors.toList());
        for (Payment payment : payments) {
            if (payment.getStatus() == PaymentStatus.COMPLETED && paidOrderIds.contains(payment.getOrderId())) {
                String methodStr = payment.getPaymentMethod().name();
                paymentMethodBreakdown.put(methodStr, paymentMethodBreakdown.getOrDefault(methodStr, BigDecimal.ZERO).add(payment.getAmount().amount()));
            }
        }

        // Cash variance from closed sessions in date range
        List<PosSession> sessions = posSessionRepository.findByCafeId(cafeId);
        BigDecimal cashVariance = sessions.stream()
                .filter(PosSession::isClosed)
                .filter(s -> s.getClosedAt() != null && isWithinTimezoneRange(s.getClosedAt(), startDate, endDate, cafeZone))
                .map(s -> s.getClosingVarianceAmount() != null ? s.getClosingVarianceAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SalesSummaryProjection(
                totalOrders,
                grossSales,
                discounts,
                tax,
                netRevenue,
                paymentMethodBreakdown,
                cashVariance
        );
    }

    public List<TopProductProjection> getTopProducts(UUID cafeId, Instant startDate, Instant endDate, int limit) {
        ZoneId cafeZone = getCafeZoneId(cafeId);
        List<Order> orders = orderRepository.findByCafeId(cafeId);

        Map<UUID, ProductAgg> aggMap = new HashMap<>();

        orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID)
                .filter(o -> isWithinTimezoneRange(o.getCreatedAt(), startDate, endDate, cafeZone))
                .flatMap(o -> o.getLines().stream())
                .forEach(line -> {
                    ProductAgg agg = aggMap.computeIfAbsent(line.getProductId(), id -> new ProductAgg(line.getProductId(), line.getProductNameSnapshot(), "Uncategorized"));
                    agg.quantity += line.getQuantity();
                    agg.revenue = agg.revenue.add(line.getLineTotalAmount());
                });

        int effectiveLimit = limit > 0 ? limit : 10;

        return aggMap.values().stream()
                .sorted((a, b) -> Long.compare(b.quantity, a.quantity))
                .limit(effectiveLimit)
                .map(a -> new TopProductProjection(a.productId, a.productName, a.categoryName, a.quantity, a.revenue))
                .collect(Collectors.toList());
    }

    @Transactional
    public ExportJob triggerExport(UUID cafeId, ExportType exportType, String format) {
        ExportJob job = new ExportJob(cafeId, exportType, format);
        ExportJob saved = exportJobRepository.save(job);
        exportJobRunner.runExportAsync(saved.getId());
        return saved;
    }

    public ExportJob getExportJob(UUID cafeId, UUID jobId) {
        return exportJobRepository.findByIdAndCafeId(jobId, cafeId)
                .orElseThrow(() -> new IllegalArgumentException("Export job not found with id: " + jobId));
    }

    private ZoneId getCafeZoneId(UUID cafeId) {
        return cafeRepository.findById(cafeId)
                .map(c -> {
                    try {
                        return ZoneId.of(c.getTimezone());
                    } catch (Exception e) {
                        return ZoneId.of("UTC");
                    }
                })
                .orElse(ZoneId.of("UTC"));
    }

    private boolean isWithinTimezoneRange(Instant time, Instant start, Instant end, ZoneId zoneId) {
        if (start == null && end == null) return true;
        if (time == null) time = Instant.now();
        ZonedDateTime zdt = time.atZone(zoneId);
        if (start != null && zdt.toInstant().isBefore(start)) return false;
        if (end != null && zdt.toInstant().isAfter(end)) return false;
        return true;
    }

    private static class ProductAgg {
        UUID productId;
        String productName;
        String categoryName;
        long quantity;
        BigDecimal revenue = BigDecimal.ZERO;

        ProductAgg(UUID productId, String productName, String categoryName) {
            this.productId = productId;
            this.productName = productName;
            this.categoryName = categoryName;
        }
    }
}
