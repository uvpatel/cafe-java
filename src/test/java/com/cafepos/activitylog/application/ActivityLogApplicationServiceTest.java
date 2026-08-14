package com.cafepos.activitylog.application;

import com.cafepos.activitylog.domain.ActivityLog;
import com.cafepos.activitylog.domain.ActivityLogRepository;
import com.cafepos.activitylog.domain.events.CouponRedeemedEvent;
import com.cafepos.activitylog.domain.events.ExportTriggeredEvent;
import com.cafepos.activitylog.domain.events.OrderLineChangedEvent;
import com.cafepos.activitylog.domain.events.PaymentProcessedEvent;
import com.cafepos.activitylog.domain.events.PosSessionClosedEvent;
import com.cafepos.activitylog.domain.events.PosSessionOpenedEvent;
import com.cafepos.activitylog.domain.events.RefundProcessedEvent;
import com.cafepos.activitylog.domain.events.UserOnboardedEvent;
import com.cafepos.kitchen.domain.KitchenTicketDispatchedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityLogApplicationServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ActivityLogApplicationService activityLogApplicationService;

    private AuditEventListener auditEventListener;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        auditEventListener = new AuditEventListener(activityLogApplicationService);
    }

    @Test
    @DisplayName("recordActivity should sanitize sensitive details and save ActivityLog")
    void recordActivityShouldSanitizeAndSave() {
        String sensitiveJson = "{\"email\":\"admin@cafe.com\",\"password\":\"secretPass123\",\"cardNumber\":\"4111222233334444\",\"cvv\":\"123\",\"amount\":45.50}";

        when(activityLogRepository.save(any(ActivityLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ActivityLog log = activityLogApplicationService.recordActivity(
                cafeId,
                employeeId,
                "admin@cafe.com",
                "USER_LOGIN",
                "EMPLOYEE",
                employeeId.toString(),
                sensitiveJson,
                "127.0.0.1"
        );

        assertThat(log).isNotNull();
        assertThat(log.getCafeId()).isEqualTo(cafeId);
        assertThat(log.getActorEmployeeId()).isEqualTo(employeeId);
        assertThat(log.getActorEmail()).isEqualTo("admin@cafe.com");
        assertThat(log.getDetailsJson()).doesNotContain("secretPass123");
        assertThat(log.getDetailsJson()).doesNotContain("4111222233334444");
        assertThat(log.getDetailsJson()).contains("[REDACTED]");
        assertThat(log.getDetailsJson()).contains("admin@cafe.com");

        verify(activityLogRepository).save(any(ActivityLog.class));
    }

    @Test
    @DisplayName("sanitizeDetails should mask sensitive credential and payment fields")
    void sanitizeDetailsShouldMaskSensitiveFields() {
        String rawJson = "{\"user\":\"john\",\"pin\":\"1234\",\"secret\":\"mySecretKey\",\"token\":\"abc.xyz.123\",\"item\":\"Coffee\"}";
        String sanitized = activityLogApplicationService.sanitizeDetails(rawJson);

        assertThat(sanitized).contains("\"user\":\"john\"");
        assertThat(sanitized).contains("\"item\":\"Coffee\"");
        assertThat(sanitized).contains("\"pin\":\"[REDACTED]\"");
        assertThat(sanitized).contains("\"secret\":\"[REDACTED]\"");
        assertThat(sanitized).contains("\"token\":\"[REDACTED]\"");
    }

    @Test
    @DisplayName("searchActivityLogs should return paged logs for cafeId")
    void searchActivityLogsShouldReturnPagedLogs() {
        Pageable pageable = PageRequest.of(0, 10);
        ActivityLog log = new ActivityLog(cafeId, employeeId, "test@cafe.com", "ACTION", "TYPE", "ID", null, null);
        Page<ActivityLog> page = new PageImpl<>(List.of(log), pageable, 1);

        when(activityLogRepository.findByCafeIdOrderByCreatedAtDesc(cafeId, pageable)).thenReturn(page);

        Page<ActivityLog> result = activityLogApplicationService.searchActivityLogs(cafeId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getCafeId()).isEqualTo(cafeId);
    }

    @Test
    @DisplayName("AuditEventListener should handle all material system events")
    void auditEventListenerShouldHandleEvents() {
        when(activityLogRepository.save(any(ActivityLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID newEmpId = UUID.randomUUID();
        auditEventListener.handleUserOnboarded(new UserOnboardedEvent(
                cafeId, employeeId, "admin@cafe.com", newEmpId, "new@cafe.com", "CASHIER", "{}", "127.0.0.1", Instant.now()
        ));

        UUID sessionId = UUID.randomUUID();
        UUID terminalId = UUID.randomUUID();
        auditEventListener.handlePosSessionOpened(new PosSessionOpenedEvent(
                cafeId, employeeId, "cashier@cafe.com", sessionId, terminalId, "{}", "127.0.0.1", Instant.now()
        ));

        auditEventListener.handlePosSessionClosed(new PosSessionClosedEvent(
                cafeId, employeeId, "cashier@cafe.com", sessionId, "{}", "127.0.0.1", Instant.now()
        ));

        UUID orderId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();
        auditEventListener.handleOrderLineChanged(new OrderLineChangedEvent(
                cafeId, employeeId, "cashier@cafe.com", orderId, lineId, "ADD", "{}", "127.0.0.1", Instant.now()
        ));

        UUID ticketId = UUID.randomUUID();
        auditEventListener.handleKitchenTicketDispatched(new KitchenTicketDispatchedEvent(
                ticketId, cafeId, orderId, "ORD-100", List.of(), Instant.now()
        ));

        UUID paymentId = UUID.randomUUID();
        auditEventListener.handlePaymentProcessed(new PaymentProcessedEvent(
                cafeId, employeeId, "cashier@cafe.com", paymentId, orderId, "CASH", "{}", "127.0.0.1", Instant.now()
        ));

        UUID refundId = UUID.randomUUID();
        auditEventListener.handleRefundProcessed(new RefundProcessedEvent(
                cafeId, employeeId, "admin@cafe.com", refundId, paymentId, "{}", "127.0.0.1", Instant.now()
        ));

        UUID redemptionId = UUID.randomUUID();
        auditEventListener.handleCouponRedeemed(new CouponRedeemedEvent(
                cafeId, employeeId, "cashier@cafe.com", redemptionId, "DISCOUNT10", orderId, "{}", "127.0.0.1", Instant.now()
        ));

        UUID jobId = UUID.randomUUID();
        auditEventListener.handleExportTriggered(new ExportTriggeredEvent(
                cafeId, employeeId, "owner@cafe.com", jobId, "SALES_SUMMARY", "CSV", "{}", "127.0.0.1", Instant.now()
        ));

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository, org.mockito.Mockito.times(9)).save(captor.capture());

        List<ActivityLog> savedLogs = captor.getAllValues();
        assertThat(savedLogs).extracting(ActivityLog::getAction).containsExactly(
                "USER_ONBOARDED",
                "SESSION_OPENED",
                "SESSION_CLOSED",
                "ORDER_LINE_CHANGED",
                "KITCHEN_DISPATCH",
                "PAYMENT_PROCESSED",
                "REFUND_PROCESSED",
                "COUPON_REDEEMED",
                "EXPORT_TRIGGERED"
        );
    }
}
