package com.cafepos.payment.api;

import com.cafepos.payment.domain.PaymentMethod;
import com.cafepos.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID cafeId,
        UUID orderId,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        String currency,
        BigDecimal tenderedAmount,
        BigDecimal changeAmount,
        String transactionReference,
        PaymentStatus status,
        Instant createdAt
) {
}
