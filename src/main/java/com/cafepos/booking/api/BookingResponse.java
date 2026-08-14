package com.cafepos.booking.api;

import com.cafepos.booking.domain.BookingStatus;

import java.time.Instant;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID cafeId,
        UUID diningTableId,
        String customerName,
        String customerPhone,
        int partySize,
        Instant bookingStart,
        Instant bookingEnd,
        BookingStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
