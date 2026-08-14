package com.cafepos.ordering.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderLineRequest(
        @NotNull(message = "Quantity must not be null")
        @Min(value = 0, message = "Quantity cannot be negative")
        Integer quantity
) {
}
