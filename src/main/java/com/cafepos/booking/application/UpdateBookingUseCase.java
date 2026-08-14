package com.cafepos.booking.application;

import com.cafepos.booking.api.BookingResponse;
import com.cafepos.booking.api.UpdateBookingRequest;

import java.util.UUID;

public interface UpdateBookingUseCase {
    BookingResponse updateBooking(UUID cafeId, UUID bookingId, UpdateBookingRequest request);
}
