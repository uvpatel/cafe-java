package com.cafepos.ordering.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddOrderLineRequest(
        @NotNull(message = "Product ID must not be null")
        UUID productId,

        @NotNull(message = "Quantity must not be null")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        java.util.List<UUID> selectedModifierOptionIds
) {
    public AddOrderLineRequest(UUID productId, Integer quantity) {
        this(productId, quantity, java.util.List.of());
    }
}


