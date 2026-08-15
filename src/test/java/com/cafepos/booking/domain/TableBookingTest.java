package com.cafepos.booking.domain;

import com.cafepos.shared.error.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TableBookingTest {

    private final UUID cafeId = UUID.randomUUID();
    private final UUID diningTableId = UUID.randomUUID();

    @Nested
    @DisplayName("Creation and Validation Invariant Tests")
    class CreationAndValidationTests {

        @Test
        @DisplayName("Should create TableBooking with default CONFIRMED status")
        void shouldCreateBookingSuccessfully() {
            Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
            Instant end = start.plus(2, ChronoUnit.HOURS);

            TableBooking booking = new TableBooking(
                    cafeId, diningTableId, "Alice Smith", "+1234567890", 4, start, end
            );

            assertThat(booking.getId()).isNotNull();
            assertThat(booking.getCafeId()).isEqualTo(cafeId);
            assertThat(booking.getDiningTableId()).isEqualTo(diningTableId);
            assertThat(booking.getCustomerName()).isEqualTo("Alice Smith");
            assertThat(booking.getCustomerPhone()).isEqualTo("+1234567890");
            assertThat(booking.getPartySize()).isEqualTo(4);
            assertThat(booking.getBookingStart()).isEqualTo(start);
            assertThat(booking.getBookingEnd()).isEqualTo(end);
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when required fields are missing or invalid")
        void shouldThrowExceptionOnInvalidFields() {
            Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
            Instant end = start.plus(2, ChronoUnit.HOURS);

            assertThatThrownBy(() -> new TableBooking(null, diningTableId, "Alice", "+123", 2, start, end))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cafe ID must not be null");

            assertThatThrownBy(() -> new TableBooking(cafeId, null, "Alice", "+123", 2, start, end))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Dining table ID must not be null");

            assertThatThrownBy(() -> new TableBooking(cafeId, diningTableId, "", "+123", 2, start, end))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Customer name must not be empty");

            assertThatThrownBy(() -> new TableBooking(cafeId, diningTableId, "Alice", " ", 2, start, end))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Customer phone must not be empty");

            assertThatThrownBy(() -> new TableBooking(cafeId, diningTableId, "Alice", "+123", 0, start, end))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Party size must be greater than 0");
        }

        @Test
        @DisplayName("Should throw BusinessException with INVALID_BOOKING_INTERVAL when start >= end")
        void shouldThrowExceptionOnInvalidInterval() {
            Instant start = Instant.now().plus(2, ChronoUnit.HOURS);
            Instant end = Instant.now().plus(1, ChronoUnit.HOURS);

            assertThatThrownBy(() -> new TableBooking(cafeId, diningTableId, "Alice", "+123", 2, start, end))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("INVALID_BOOKING_INTERVAL");
                        assertThat(be.getStatus()).isEqualTo(400);
                    });
        }
    }

    @Nested
    @DisplayName("Status Transition Tests")
    class StatusTransitionTests {

        @Test
        @DisplayName("Should transition CONFIRMED booking to CONVERTED_TO_ORDER")
        void shouldConvertToOrder() {
            Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
            Instant end = start.plus(2, ChronoUnit.HOURS);
            TableBooking booking = new TableBooking(cafeId, diningTableId, "Bob", "+9876543210", 2, start, end);

            booking.convertToOrder();

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONVERTED_TO_ORDER);
        }

        @Test
        @DisplayName("Should transition CONFIRMED booking to CANCELLED")
        void shouldCancelBooking() {
            Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
            Instant end = start.plus(2, ChronoUnit.HOURS);
            TableBooking booking = new TableBooking(cafeId, diningTableId, "Bob", "+9876543210", 2, start, end);

            booking.cancel();

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should transition CONFIRMED booking to EXPIRED when hold time lapses")
        void shouldExpireBooking() {
            Instant start = Instant.now().minus(2, ChronoUnit.HOURS);
            Instant end = start.plus(1, ChronoUnit.HOURS);
            TableBooking booking = new TableBooking(cafeId, diningTableId, "Bob", "+9876543210", 2, start, end);

            booking.expire();

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        }

        @Test
        @DisplayName("Should fail transition to order if booking is already CANCELLED or CONVERTED_TO_ORDER")
        void shouldFailConversionIfAlreadyFinalized() {
            Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
            Instant end = start.plus(2, ChronoUnit.HOURS);

            TableBooking booking1 = new TableBooking(cafeId, diningTableId, "Bob", "+9876543210", 2, start, end);
            booking1.cancel();

            assertThatThrownBy(booking1::convertToOrder)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("BOOKING_NOT_CONFIRMED");
                        assertThat(be.getStatus()).isEqualTo(400);
                    });

            TableBooking booking2 = new TableBooking(cafeId, diningTableId, "Bob", "+9876543210", 2, start, end);
            booking2.convertToOrder();

            assertThatThrownBy(booking2::cancel)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("BOOKING_NOT_CONFIRMED");
                        assertThat(be.getStatus()).isEqualTo(400);
                    });
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Bookings with same ID should be equal")
        void shouldBeEqualForSameId() {
            UUID bookingId = UUID.randomUUID();
            Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
            Instant end = start.plus(2, ChronoUnit.HOURS);

            TableBooking b1 = new TableBooking(bookingId, cafeId, diningTableId, "Alice", "+123", 2, start, end, BookingStatus.CONFIRMED);
            TableBooking b2 = new TableBooking(bookingId, cafeId, diningTableId, "Bob", "+987", 4, start, end, BookingStatus.CONFIRMED);

            assertThat(b1).isEqualTo(b2);
            assertThat(b1.hashCode()).isEqualTo(b2.hashCode());
        }
    }
}
