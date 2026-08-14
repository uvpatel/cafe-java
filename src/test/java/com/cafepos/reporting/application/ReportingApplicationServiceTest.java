package com.cafepos.reporting.application;

import com.cafepos.organization.domain.Cafe;
import com.cafepos.organization.domain.CafeRepository;
import com.cafepos.ordering.domain.Order;
import com.cafepos.ordering.domain.OrderRepository;
import com.cafepos.ordering.domain.OrderType;
import com.cafepos.payment.domain.Payment;
import com.cafepos.payment.domain.PaymentMethod;
import com.cafepos.payment.domain.PaymentRepository;
import com.cafepos.reporting.domain.ExportJob;
import com.cafepos.reporting.domain.ExportJobRepository;
import com.cafepos.reporting.domain.ExportType;
import com.cafepos.session.domain.PosSession;
import com.cafepos.session.domain.PosSessionRepository;
import com.cafepos.shared.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingApplicationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PosSessionRepository posSessionRepository;

    @Mock
    private CafeRepository cafeRepository;

    @Mock
    private ExportJobRepository exportJobRepository;

    @Mock
    private ExportJobRunner exportJobRunner;

    @InjectMocks
    private ReportingApplicationService reportingApplicationService;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID posSessionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("getSalesSummary should correctly aggregate paid orders and payments")
    void getSalesSummaryShouldAggregatePaidOrders() {
        Cafe cafe = new Cafe(cafeId, "Test Cafe", "test-cafe", "UTC", "USD");
        when(cafeRepository.findById(cafeId)).thenReturn(Optional.of(cafe));

        Order order = new Order(cafeId, posSessionId, null, "ORD-1", OrderType.TAKEAWAY);
        order.addOrUpdateLine(UUID.randomUUID(), "Espresso", Money.of(new BigDecimal("5.00")), 2, false);
        order.confirm();
        order.markPaid();

        when(orderRepository.findByCafeId(cafeId)).thenReturn(List.of(order));

        Payment payment = new Payment(cafeId, order.getId(), PaymentMethod.CASH, Money.of(new BigDecimal("10.00")), Money.of(new BigDecimal("10.00")), "REF1");
        when(paymentRepository.findByCafeId(cafeId)).thenReturn(List.of(payment));

        PosSession session = new PosSession(cafeId, UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN);
        session.closeSession(UUID.randomUUID(), BigDecimal.valueOf(100), BigDecimal.valueOf(98)); // variance -2
        when(posSessionRepository.findByCafeId(cafeId)).thenReturn(List.of(session));

        SalesSummaryProjection summary = reportingApplicationService.getSalesSummary(cafeId, null, null);

        assertThat(summary.totalOrders()).isEqualTo(1);
        assertThat(summary.grossSales()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(summary.netRevenue()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(summary.paymentMethodBreakdown().get("CASH")).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(summary.cashVariance()).isEqualByComparingTo(new BigDecimal("-2.00"));
    }

    @Test
    @DisplayName("getTopProducts should rank products by quantity sold")
    void getTopProductsShouldRankByQuantity() {
        Cafe cafe = new Cafe(cafeId, "Test Cafe", "test-cafe", "UTC", "USD");
        when(cafeRepository.findById(cafeId)).thenReturn(Optional.of(cafe));

        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        Order order = new Order(cafeId, posSessionId, null, "ORD-1", OrderType.TAKEAWAY);
        order.addOrUpdateLine(p1, "Latte", Money.of(new BigDecimal("4.00")), 5, false);
        order.addOrUpdateLine(p2, "Cookie", Money.of(new BigDecimal("2.00")), 2, false);
        order.confirm();
        order.markPaid();

        when(orderRepository.findByCafeId(cafeId)).thenReturn(List.of(order));

        List<TopProductProjection> topProducts = reportingApplicationService.getTopProducts(cafeId, null, null, 10);

        assertThat(topProducts).hasSize(2);
        assertThat(topProducts.get(0).productId()).isEqualTo(p1);
        assertThat(topProducts.get(0).totalQuantitySold()).isEqualTo(5);
        assertThat(topProducts.get(1).productId()).isEqualTo(p2);
        assertThat(topProducts.get(1).totalQuantitySold()).isEqualTo(2);
    }

    @Test
    @DisplayName("triggerExport should save ExportJob and run async execution")
    void triggerExportShouldSaveAndRunAsync() {
        ExportJob job = new ExportJob(cafeId, ExportType.SALES_SUMMARY, "CSV");
        when(exportJobRepository.save(any(ExportJob.class))).thenReturn(job);

        ExportJob result = reportingApplicationService.triggerExport(cafeId, ExportType.SALES_SUMMARY, "CSV");

        assertThat(result).isNotNull();
        verify(exportJobRepository).save(any(ExportJob.class));
        verify(exportJobRunner).runExportAsync(job.getId());
    }

    @Test
    @DisplayName("getExportJob should return export job if exists or throw exception")
    void getExportJobShouldReturnJobOrThrow() {
        UUID jobId = UUID.randomUUID();
        ExportJob job = new ExportJob(jobId, cafeId, ExportType.SALES_SUMMARY, "CSV");

        when(exportJobRepository.findByIdAndCafeId(jobId, cafeId)).thenReturn(Optional.of(job));

        ExportJob found = reportingApplicationService.getExportJob(cafeId, jobId);
        assertThat(found.getId()).isEqualTo(jobId);

        when(exportJobRepository.findByIdAndCafeId(jobId, cafeId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reportingApplicationService.getExportJob(cafeId, jobId))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
