package com.cafepos.session.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CloseSessionRequest(
        @NotNull(message = "Counted amount must not be null")
        @DecimalMin(value = "0.00", message = "Counted amount must not be negative")
        BigDecimal countedAmount
) {
}
