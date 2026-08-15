package com.cafepos.ordering.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderLineResponse(
        UUID id,
        UUID orderId,
        UUID productId,
        String productNameSnapshot,
        BigDecimal unitPriceSnapshot,
        int quantity,
        BigDecimal lineSubtotal,
        BigDecimal lineTotal,
        boolean isKitchenRouted,
        Instant createdAt,
        java.util.List<OrderLineModifierResponse> modifiers
) {
    public OrderLineResponse(UUID id, UUID orderId, UUID productId, String productNameSnapshot, BigDecimal unitPriceSnapshot, int quantity, BigDecimal lineSubtotal, BigDecimal lineTotal, boolean isKitchenRouted, Instant createdAt) {
        this(id, orderId, productId, productNameSnapshot, unitPriceSnapshot, quantity, lineSubtotal, lineTotal, isKitchenRouted, createdAt, java.util.List.of());
    }
}


