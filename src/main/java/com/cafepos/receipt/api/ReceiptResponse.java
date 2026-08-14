package com.cafepos.receipt.api;

import com.cafepos.ordering.domain.OrderStatus;
import com.cafepos.ordering.domain.OrderType;
import com.cafepos.payment.domain.PaymentMethod;
import com.cafepos.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReceiptResponse(
        UUID orderId,
        String orderNumber,
        UUID cafeId,
        UUID posSessionId,
        UUID diningTableId,
        OrderType orderType,
        OrderStatus orderStatus,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String currency,
        String notes,
        Instant orderCreatedAt,
        Instant receiptGeneratedAt,
        ReceiptPaymentInfo payment,
        List<ReceiptLineResponse> lines,
        List<ReceiptTaxLineResponse> taxLines
) {
    public record ReceiptPaymentInfo(
            UUID paymentId,
            PaymentMethod paymentMethod,
            BigDecimal amount,
            BigDecimal tenderedAmount,
            BigDecimal changeAmount,
            String transactionReference,
            PaymentStatus status,
            Instant createdAt
    ) {}

    public record ReceiptLineResponse(
            UUID lineId,
            UUID productId,
            String productName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineSubtotal,
            BigDecimal lineTotal
    ) {}

    public record ReceiptTaxLineResponse(
            UUID taxLineId,
            String taxName,
            BigDecimal ratePercent,
            BigDecimal taxAmount
    ) {}
}
