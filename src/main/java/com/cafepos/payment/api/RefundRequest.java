package com.cafepos.payment.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RefundRequest(
        @NotNull(message = "Refund amount must not be null")
        @DecimalMin(value = "0.01", message = "Refund amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Refund reason must not be blank")
        String reason
) {
}
