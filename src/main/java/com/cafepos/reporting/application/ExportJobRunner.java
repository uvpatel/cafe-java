package com.cafepos.reporting.application;

import com.cafepos.ordering.domain.Order;
import com.cafepos.ordering.domain.OrderRepository;
import com.cafepos.ordering.domain.OrderStatus;
import com.cafepos.payment.domain.Payment;
import com.cafepos.payment.domain.PaymentRepository;
import com.cafepos.session.domain.PosSession;
import com.cafepos.session.domain.PosSessionRepository;
import com.cafepos.reporting.domain.ExportJob;
import com.cafepos.reporting.domain.ExportJobRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExportJobRunner {

    private static final Logger log = LoggerFactory.getLogger(ExportJobRunner.class);

    private final ExportJobRepository exportJobRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PosSessionRepository posSessionRepository;

    public ExportJobRunner(ExportJobRepository exportJobRepository,
                           OrderRepository orderRepository,
                           PaymentRepository paymentRepository,
                           PosSessionRepository posSessionRepository) {
        this.exportJobRepository = exportJobRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.posSessionRepository = posSessionRepository;
    }

    @Async
    @Transactional
    public void runExportAsync(UUID jobId) {
        ExportJob job = exportJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.error("ExportJob not found for ID: {}", jobId);
            return;
        }

        try {
            job.markProcessing();
            exportJobRepository.save(job);

            Path tempDir = Files.createTempDirectory("cafepos_exports");
            String extension = "xlsx".equalsIgnoreCase(job.getFormat()) ? ".xlsx" : ".csv";
            File exportFile = File.createTempFile("export_" + job.getId(), extension, tempDir.toFile());

            generateReportFile(job, exportFile);

            job.markCompleted(exportFile.getAbsolutePath());
            exportJobRepository.save(job);
            log.info("ExportJob {} completed successfully at {}", jobId, exportFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to execute export job {}", jobId, e);
            job.markFailed();
            exportJobRepository.save(job);
        }
    }

    private void generateReportFile(ExportJob job, File exportFile) throws Exception {
        try (PrintWriter writer = new PrintWriter(new FileWriter(exportFile))) {
            switch (job.getExportType()) {
                case SALES_SUMMARY -> writeSalesSummary(job.getCafeId(), writer);
                case TOP_PRODUCTS -> writeTopProducts(job.getCafeId(), writer);
                case SHIFT_RECONCILIATION -> writeShiftReconciliation(job.getCafeId(), writer);
                case PAYMENT_BREAKDOWN -> writePaymentBreakdown(job.getCafeId(), writer);
            }
        }
    }

    private void writePaymentBreakdown(UUID cafeId, PrintWriter writer) {
        writer.println("Payment ID,Order ID,Payment Method,Amount,Status,Processed At");
        List<Payment> payments = paymentRepository.findByCafeId(cafeId);
        for (Payment p : payments) {
            writer.println(p.getId() + "," + p.getOrderId() + "," + p.getPaymentMethod() + "," + p.getAmount().amount().toPlainString() + "," + p.getStatus() + "," + p.getCreatedAt());
        }
    }

    private void writeSalesSummary(UUID cafeId, PrintWriter writer) {
        writer.println("Metric,Value");
        List<Order> orders = orderRepository.findByCafeId(cafeId).stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID)
                .collect(Collectors.toList());

        long totalOrders = orders.size();
        BigDecimal grossSales = orders.stream().map(o -> o.getSubtotal().amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discounts = orders.stream().map(o -> o.getDiscount().amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = orders.stream().map(o -> o.getTax().amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netRevenue = orders.stream().map(o -> o.getTotal().amount()).reduce(BigDecimal.ZERO, BigDecimal::add);

        writer.println(FormulaSanitizer.sanitize("Total Orders") + "," + totalOrders);
        writer.println(FormulaSanitizer.sanitize("Gross Sales") + "," + grossSales.toPlainString());
        writer.println(FormulaSanitizer.sanitize("Discounts") + "," + discounts.toPlainString());
        writer.println(FormulaSanitizer.sanitize("Tax") + "," + tax.toPlainString());
        writer.println(FormulaSanitizer.sanitize("Net Revenue") + "," + netRevenue.toPlainString());
    }

    private void writeTopProducts(UUID cafeId, PrintWriter writer) {
        writer.println("Product ID,Product Name,Quantity Sold,Total Revenue");
        List<Order> orders = orderRepository.findByCafeId(cafeId).stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID)
                .collect(Collectors.toList());

        Map<UUID, ProductAgg> aggMap = new HashMap<>();
        orders.stream()
                .flatMap(o -> o.getLines().stream())
                .forEach(line -> {
                    ProductAgg agg = aggMap.computeIfAbsent(line.getProductId(), id -> new ProductAgg(line.getProductId(), line.getProductNameSnapshot()));
                    agg.quantity += line.getQuantity();
                    agg.revenue = agg.revenue.add(line.getLineTotalAmount());
                });

        for (ProductAgg agg : aggMap.values()) {
            writer.println(agg.productId + "," + FormulaSanitizer.sanitize(agg.productName) + "," + agg.quantity + "," + agg.revenue.toPlainString());
        }
    }

    private void writeShiftReconciliation(UUID cafeId, PrintWriter writer) {
        writer.println("Session ID,Status,Opened At,Closed At,Opening Balance,Expected Closing,Counted Closing,Variance");
        List<PosSession> sessions = posSessionRepository.findByCafeId(cafeId);
        for (PosSession s : sessions) {
            String opening = s.getOpeningBalanceAmount() != null ? s.getOpeningBalanceAmount().toPlainString() : "0";
            String expected = s.getClosingExpectedAmount() != null ? s.getClosingExpectedAmount().toPlainString() : "0";
            String counted = s.getClosingCountedAmount() != null ? s.getClosingCountedAmount().toPlainString() : "0";
            String variance = s.getClosingVarianceAmount() != null ? s.getClosingVarianceAmount().toPlainString() : "0";
            writer.println(s.getId() + "," + s.getStatus() + "," + s.getOpenedAt() + "," + (s.getClosedAt() != null ? s.getClosedAt() : "") + "," + opening + "," + expected + "," + counted + "," + variance);
        }
    }

    private static class ProductAgg {
        UUID productId;
        String productName;
        long quantity;
        BigDecimal revenue = BigDecimal.ZERO;

        ProductAgg(UUID productId, String productName) {
            this.productId = productId;
            this.productName = productName;
        }
    }
}
