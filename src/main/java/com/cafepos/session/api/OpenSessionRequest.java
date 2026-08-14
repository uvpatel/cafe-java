package com.cafepos.session.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record OpenSessionRequest(
        @NotNull(message = "Terminal ID must not be null")
        UUID terminalId,

        @NotNull(message = "Opening balance amount must not be null")
        @DecimalMin(value = "0.00", message = "Opening balance amount must not be negative")
        BigDecimal openingBalanceAmount
) {
}
