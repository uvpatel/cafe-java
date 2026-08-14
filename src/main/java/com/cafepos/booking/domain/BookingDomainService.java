package com.cafepos.booking.domain;

import com.cafepos.shared.error.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class BookingDomainService {

    private final TableBookingRepository tableBookingRepository;

    public BookingDomainService(TableBookingRepository tableBookingRepository) {
        this.tableBookingRepository = Objects.requireNonNull(tableBookingRepository, "TableBookingRepository must not be null");
    }

    public void validateBookingInterval(Instant bookingStart, Instant bookingEnd) {
        if (bookingStart == null) {
            throw new IllegalArgumentException("Booking start time must not be null");
        }
        if (bookingEnd == null) {
            throw new IllegalArgumentException("Booking end time must not be null");
        }
        if (!bookingStart.isBefore(bookingEnd)) {
            throw new BusinessException("INVALID_BOOKING_INTERVAL", "Booking start time must be before end time", 400);
        }
    }

    public void checkForOverlappingBookings(UUID cafeId, UUID diningTableId, Instant bookingStart, Instant bookingEnd, UUID excludeBookingId) {
        validateBookingInterval(bookingStart, bookingEnd);
        List<TableBooking> overlapping = tableBookingRepository.findOverlappingBookingsExcluding(
                cafeId, diningTableId, bookingStart, bookingEnd, excludeBookingId
        );
        if (!overlapping.isEmpty()) {
            throw new BusinessException("BOOKING_OVERLAP", "Dining table has overlapping booking for requested time slot", 409);
        }
    }

    @Transactional
    public TableBooking createBooking(UUID cafeId, UUID diningTableId, String customerName, String customerPhone,
                                       int partySize, Instant bookingStart, Instant bookingEnd) {
        checkForOverlappingBookings(cafeId, diningTableId, bookingStart, bookingEnd, null);
        TableBooking booking = new TableBooking(
                cafeId, diningTableId, customerName, customerPhone, partySize, bookingStart, bookingEnd
        );
        return tableBookingRepository.save(booking);
    }

    @Transactional
    public TableBooking convertBookingToOrder(UUID bookingId, UUID cafeId) {
        Objects.requireNonNull(bookingId, "Booking ID must not be null");
        Objects.requireNonNull(cafeId, "Cafe ID must not be null");

        TableBooking booking = tableBookingRepository.findByIdAndCafeId(bookingId, cafeId)
                .orElseThrow(() -> new BusinessException("BOOKING_NOT_FOUND", "Booking not found with id " + bookingId, 404));

        booking.convertToOrder();
        return tableBookingRepository.save(booking);
    }

    @Transactional
    public TableBooking cancelBooking(UUID bookingId, UUID cafeId) {
        Objects.requireNonNull(bookingId, "Booking ID must not be null");
        Objects.requireNonNull(cafeId, "Cafe ID must not be null");

        TableBooking booking = tableBookingRepository.findByIdAndCafeId(bookingId, cafeId)
                .orElseThrow(() -> new BusinessException("BOOKING_NOT_FOUND", "Booking not found with id " + bookingId, 404));

        booking.cancel();
        return tableBookingRepository.save(booking);
    }
}
