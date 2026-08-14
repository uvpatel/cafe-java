package com.cafepos.payment.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProcessCashPaymentRequest(
        @NotNull(message = "Tendered amount must not be null")
        @DecimalMin(value = "0.01", message = "Tendered amount must be greater than zero")
        BigDecimal tenderedAmount
) {
}
