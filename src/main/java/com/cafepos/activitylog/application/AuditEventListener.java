package com.cafepos.activitylog.application;

import com.cafepos.activitylog.domain.events.CouponRedeemedEvent;
import com.cafepos.activitylog.domain.events.ExportTriggeredEvent;
import com.cafepos.activitylog.domain.events.OrderLineChangedEvent;
import com.cafepos.activitylog.domain.events.PaymentProcessedEvent;
import com.cafepos.activitylog.domain.events.PosSessionClosedEvent;
import com.cafepos.activitylog.domain.events.PosSessionOpenedEvent;
import com.cafepos.activitylog.domain.events.RefundProcessedEvent;
import com.cafepos.activitylog.domain.events.UserOnboardedEvent;
import com.cafepos.kitchen.domain.KitchenTicketDispatchedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AuditEventListener {

    private final ActivityLogApplicationService activityLogApplicationService;

    public AuditEventListener(ActivityLogApplicationService activityLogApplicationService) {
        this.activityLogApplicationService = Objects.requireNonNull(activityLogApplicationService, "activityLogApplicationService must not be null");
    }

    @EventListener
    public void handleUserOnboarded(UserOnboardedEvent event) {
        activityLogApplicationService.recordActivity(
                event.cafeId(),
                event.actorEmployeeId(),
                event.actorEmail(),
                "USER_ONBOARDED",
                "EMPLOYEE",
                event.newEmployeeId().toString(),
                event.detailsJson(),
                event.ipAddress()
        );
    }

    @EventListener
    public void handlePosSessionOpened(PosSessionOpenedEvent event) {
        activityLogApplicationService.recordActivity(
                event.cafeId(),
                event.actorEmployeeId(),
                event.actorEmail(),
                "SESSION_OPENED",
                "POS_SESSION",
                event.sessionId().toString(),
                event.detailsJson(),
                event.ipAddress()
        );
    }

    @EventListener
    public void handlePosSessionClosed(PosSessionClosedEvent event) {
        activityLogApplicationService.recordActivity(
                event.cafeId(),
                event.actorEmployeeId(),
                event.actorEmail(),
                "SESSION_CLOSED",
                "POS_SESSION",
                event.sessionId().toString(),
                event.detailsJson(),
                event.ipAddress()
        );
    }

    @EventListener
    public void handleOrderLineChanged(OrderLineChangedEvent event) {
        activityLogApplicationService.recordActivity(
                event.cafeId(),
                event.actorEmployeeId(),
                event.actorEmail(),
                "ORDER_LINE_CHANGED",
                "ORDER",
                event.orderId().toString(),
                event.detailsJson(),
                event.ipAddress()
        );
    }

    @EventListener
    public void handleKitchenTicketDispatched(KitchenTicketDispatchedEvent event) {
        String detailsJson = String.format("{\"orderId\":\"%s\",\"orderNumber\":\"%s\",\"itemsCount\":%d}",
                event.orderId(), event.orderNumber(), event.items() != null ? event.items().size() : 0);

        activityLogApplicationService.recordActivity(
                event.cafeId(),
                null,
                "system@cafepos.com",
                "KITCHEN_DISPATCH",
                "KITCHEN_TICKET",
                event.ticketId().toString(),
                detailsJson,
                null
        );
    }

    @EventListener
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        activityLogApplicationService.recordActivity(
                event.cafeId(),
                event.actorEmployeeId(),
                event.actorEmail(),
                "PAYMENT_PROCESSED",
                "PAYMENT",
                event.paymentId().toString(),
                event.detailsJson(),
                event.ipAddress()
        );
    }

    @EventListener
    public void handleRefundProcessed(RefundProcessedEvent event) {
        activityLogApplicationService.recordActivity(
                event.cafeId(),
                event.actorEmployeeId(),
                event.actorEmail(),
                "REFUND_PROCESSED",
                "REFUND",
                event.refundId().toString(),
                event.detailsJson(),
                event.ipAddress()
        );
    }

    @EventListener
    public void handleCouponRedeemed(CouponRedeemedEvent event) {
        activityLogApplicationService.recordActivity(
                event.cafeId(),
                event.actorEmployeeId(),
                event.actorEmail(),
                "COUPON_REDEEMED",
                "COUPON_REDEMPTION",
                event.redemptionId().toString(),
                event.detailsJson(),
                event.ipAddress()
        );
    }

    @EventListener
    public void handleExportTriggered(ExportTriggeredEvent event) {
        activityLogApplicationService.recordActivity(
                event.cafeId(),
                event.actorEmployeeId(),
                event.actorEmail(),
                "EXPORT_TRIGGERED",
                "EXPORT_JOB",
                event.jobId().toString(),
                event.detailsJson(),
                event.ipAddress()
        );
    }
}
