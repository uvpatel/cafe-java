package com.cafepos.ordering.api;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignTableRequest(
        @NotNull(message = "Dining table ID must not be null")
        UUID diningTableId
) {
}
