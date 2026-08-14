package com.cafepos.session.api;

import com.cafepos.session.domain.CashMovementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RecordCashMovementRequest(
        @NotNull(message = "Cash movement type must not be null")
        CashMovementType type,

        @NotNull(message = "Amount must not be null")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Reason must not be blank")
        @Size(max = 255, message = "Reason must not exceed 255 characters")
        String reason
) {
}
