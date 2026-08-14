package com.cafepos.booking.application;

import com.cafepos.booking.api.BookingResponse;

import java.util.UUID;

public interface CancelBookingUseCase {
    BookingResponse cancelBooking(UUID cafeId, UUID bookingId);
}
