package com.cafepos.booking.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateBookingRequest(
        @NotBlank(message = "Customer name must not be empty")
        @Size(max = 100, message = "Customer name must not exceed 100 characters")
        String customerName,

        @NotBlank(message = "Customer phone must not be empty")
        @Size(max = 30, message = "Customer phone must not exceed 30 characters")
        String customerPhone,

        @Min(value = 1, message = "Party size must be greater than 0")
        int partySize,

        @NotNull(message = "Booking start time must not be null")
        Instant bookingStart,

        @NotNull(message = "Booking end time must not be null")
        Instant bookingEnd
) {
}
