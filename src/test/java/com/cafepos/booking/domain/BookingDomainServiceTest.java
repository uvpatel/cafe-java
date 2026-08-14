package com.cafepos.booking.domain;

import com.cafepos.shared.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingDomainServiceTest {

    @Mock
    private TableBookingRepository tableBookingRepository;

    private BookingDomainService bookingDomainService;

    private UUID cafeId;
    private UUID diningTableId;

    @BeforeEach
    void setUp() {
        bookingDomainService = new BookingDomainService(tableBookingRepository);
        cafeId = UUID.randomUUID();
        diningTableId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should validate interval bounds successfully when start < end")
    void shouldValidateIntervalBoundsSuccessfully() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        bookingDomainService.validateBookingInterval(start, end);
    }

    @Test
    @DisplayName("Should throw BusinessException when start >= end")
    void shouldThrowExceptionWhenIntervalIsInvalid() {
        Instant start = Instant.now().plus(2, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(1, ChronoUnit.HOURS);

        assertThatThrownBy(() -> bookingDomainService.validateBookingInterval(start, end))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("INVALID_BOOKING_INTERVAL");
                    assertThat(be.getStatus()).isEqualTo(400);
                });
    }

    @Test
    @DisplayName("Should create booking when no interval overlap exists")
    void shouldCreateBookingWhenNoOverlap() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        when(tableBookingRepository.findOverlappingBookingsExcluding(cafeId, diningTableId, start, end, null))
                .thenReturn(Collections.emptyList());
        when(tableBookingRepository.save(any(TableBooking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TableBooking booking = bookingDomainService.createBooking(cafeId, diningTableId, "Charlie Brown", "+1122334455", 3, start, end);

        assertThat(booking).isNotNull();
        assertThat(booking.getCafeId()).isEqualTo(cafeId);
        assertThat(booking.getDiningTableId()).isEqualTo(diningTableId);
        assertThat(booking.getCustomerName()).isEqualTo("Charlie Brown");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        verify(tableBookingRepository).save(any(TableBooking.class));
    }

    @Test
    @DisplayName("Should throw BOOKING_OVERLAP BusinessException (409) when interval overlap detected")
    void shouldThrowBookingOverlapWhenSlotCollisionDetected() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        TableBooking existingBooking = new TableBooking(
                cafeId, diningTableId, "Existing Guest", "+9999999999", 2, start.minus(30, ChronoUnit.MINUTES), start.plus(30, ChronoUnit.MINUTES)
        );

        when(tableBookingRepository.findOverlappingBookingsExcluding(cafeId, diningTableId, start, end, null))
                .thenReturn(List.of(existingBooking));

        assertThatThrownBy(() -> bookingDomainService.createBooking(cafeId, diningTableId, "Charlie Brown", "+1122334455", 3, start, end))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("BOOKING_OVERLAP");
                    assertThat(be.getStatus()).isEqualTo(409);
                });

        verify(tableBookingRepository, never()).save(any(TableBooking.class));
    }

    @Test
    @DisplayName("Should convert booking to active order successfully")
    void shouldConvertBookingToActiveOrder() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        TableBooking booking = new TableBooking(cafeId, diningTableId, "Diana Prince", "+5544332211", 4, start, end);
        UUID bookingId = booking.getId();

        when(tableBookingRepository.findByIdAndCafeId(bookingId, cafeId))
                .thenReturn(Optional.of(booking));
        when(tableBookingRepository.save(any(TableBooking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TableBooking converted = bookingDomainService.convertBookingToOrder(bookingId, cafeId);

        assertThat(converted.getStatus()).isEqualTo(BookingStatus.CONVERTED_TO_ORDER);
        verify(tableBookingRepository).save(booking);
    }

    @Test
    @DisplayName("Should cancel booking successfully")
    void shouldCancelBookingSuccessfully() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        TableBooking booking = new TableBooking(cafeId, diningTableId, "Eve", "+6677889900", 2, start, end);
        UUID bookingId = booking.getId();

        when(tableBookingRepository.findByIdAndCafeId(bookingId, cafeId))
                .thenReturn(Optional.of(booking));
        when(tableBookingRepository.save(any(TableBooking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TableBooking cancelled = bookingDomainService.cancelBooking(bookingId, cafeId);

        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(tableBookingRepository).save(booking);
    }
}
