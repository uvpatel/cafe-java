package com.cafepos.booking.application;

import com.cafepos.booking.api.BookingResponse;
import com.cafepos.booking.api.CreateBookingRequest;

import java.util.UUID;

public interface CreateBookingUseCase {
    BookingResponse createBooking(UUID cafeId, CreateBookingRequest request);
}
