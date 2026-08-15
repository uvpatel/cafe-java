package com.cafepos.receipt.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReceiptResponse(
        UUID id,
        UUID cafeId,
        UUID orderId,
        String receiptNumber,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal serviceChargeAmount,
        BigDecimal totalAmount,
        String currency,
        String taxBreakdownJson,
        Instant createdAt
) {
}
