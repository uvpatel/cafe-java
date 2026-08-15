package com.cafepos.ordering.api;

import com.cafepos.ordering.domain.OrderStatus;
import com.cafepos.ordering.domain.OrderType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID cafeId,
        UUID posSessionId,
        UUID diningTableId,
        String orderNumber,
        OrderType orderType,
        OrderStatus status,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal serviceChargeAmount,
        BigDecimal totalAmount,
        String currency,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        List<OrderLineResponse> lines
) {
}
