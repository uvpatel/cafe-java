package com.cafepos.catalog.api;

import com.cafepos.catalog.domain.UnitOfMeasure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID cafeId,
        UUID categoryId,
        String name,
        String sku,
        BigDecimal priceAmount,
        String currency,
        UnitOfMeasure unitOfMeasure,
        boolean kitchenRouted,
        boolean archived,
        Instant createdAt,
        Instant updatedAt
) {
}
