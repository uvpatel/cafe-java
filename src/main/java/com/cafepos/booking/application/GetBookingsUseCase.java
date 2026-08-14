package com.cafepos.booking.application;

import com.cafepos.booking.api.BookingResponse;
import com.cafepos.booking.domain.BookingStatus;

import java.util.List;
import java.util.UUID;

public interface GetBookingsUseCase {
    List<BookingResponse> getBookings(UUID cafeId, UUID diningTableId, BookingStatus status);
    BookingResponse getBookingById(UUID cafeId, UUID bookingId);
}
