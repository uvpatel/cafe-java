package com.cafepos.booking.application;

import com.cafepos.booking.api.BookingDtoMapper;
import com.cafepos.booking.api.BookingResponse;
import com.cafepos.booking.api.ConvertToOrderResponse;
import com.cafepos.booking.api.CreateBookingRequest;
import com.cafepos.booking.api.UpdateBookingRequest;
import com.cafepos.booking.domain.BookingDomainService;
import com.cafepos.booking.domain.BookingStatus;
import com.cafepos.booking.domain.TableBooking;
import com.cafepos.booking.domain.TableBookingRepository;
import com.cafepos.floor.domain.DiningTable;
import com.cafepos.floor.domain.DiningTableRepository;
import com.cafepos.ordering.api.CreateOrderRequest;
import com.cafepos.ordering.api.OrderResponse;
import com.cafepos.ordering.application.CreateOrderUseCase;
import com.cafepos.ordering.domain.OrderStatus;
import com.cafepos.ordering.domain.OrderType;
import com.cafepos.session.domain.PosSession;
import com.cafepos.session.domain.PosSessionRepository;
import com.cafepos.shared.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingApplicationServiceTest {

    @Mock
    private BookingDomainService bookingDomainService;

    @Mock
    private TableBookingRepository tableBookingRepository;

    @Mock
    private DiningTableRepository diningTableRepository;

    @Mock
    private CreateOrderUseCase createOrderUseCase;

    @Mock
    private PosSessionRepository posSessionRepository;

    private final BookingDtoMapper dtoMapper = new BookingDtoMapper();

    private BookingApplicationService bookingApplicationService;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID floorId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();
    private final UUID posSessionId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();

    private Instant now;
    private Instant bookingStart;
    private Instant bookingEnd;

    @BeforeEach
    void setUp() {
        bookingApplicationService = new BookingApplicationService(
                bookingDomainService,
                tableBookingRepository,
                diningTableRepository,
                createOrderUseCase,
                posSessionRepository,
                dtoMapper
        );

        now = Instant.now();
        bookingStart = now.plus(1, ChronoUnit.HOURS);
        bookingEnd = now.plus(2, ChronoUnit.HOURS);
    }

    @Nested
    @DisplayName("Create Booking Tests")
    class CreateBookingTests {

        @Test
        @DisplayName("Should create booking successfully when table exists and no collision")
        void shouldCreateBookingSuccessfully() {
            CreateBookingRequest request = new CreateBookingRequest(tableId, "John Doe", "+1234567890", 4, bookingStart, bookingEnd);
            DiningTable diningTable = new DiningTable(tableId, cafeId, floorId, "T1", 4, true);
            TableBooking createdBooking = new TableBooking(bookingId, cafeId, tableId, "John Doe", "+1234567890", 4, bookingStart, bookingEnd, BookingStatus.CONFIRMED);

            when(diningTableRepository.findByIdAndCafeId(tableId, cafeId)).thenReturn(Optional.of(diningTable));
            when(bookingDomainService.createBooking(cafeId, tableId, "John Doe", "+1234567890", 4, bookingStart, bookingEnd))
                    .thenReturn(createdBooking);

            BookingResponse response = bookingApplicationService.createBooking(cafeId, request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(bookingId);
            assertThat(response.customerName()).isEqualTo("John Doe");
            assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Should throw 404 when dining table does not exist")
        void shouldThrow404WhenTableNotFound() {
            CreateBookingRequest request = new CreateBookingRequest(tableId, "John Doe", "+1234567890", 4, bookingStart, bookingEnd);

            when(diningTableRepository.findByIdAndCafeId(tableId, cafeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingApplicationService.createBooking(cafeId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Dining table not found")
                    .extracting(ex -> ((BusinessException) ex).getStatus())
                    .isEqualTo(404);
        }

        @Test
        @DisplayName("Should throw 409 when booking interval overlaps with existing booking")
        void shouldThrow409WhenBookingOverlaps() {
            CreateBookingRequest request = new CreateBookingRequest(tableId, "John Doe", "+1234567890", 4, bookingStart, bookingEnd);
            DiningTable diningTable = new DiningTable(tableId, cafeId, floorId, "T1", 4, true);

            when(diningTableRepository.findByIdAndCafeId(tableId, cafeId)).thenReturn(Optional.of(diningTable));
            when(bookingDomainService.createBooking(cafeId, tableId, "John Doe", "+1234567890", 4, bookingStart, bookingEnd))
                    .thenThrow(new BusinessException("BOOKING_OVERLAP", "Dining table has overlapping booking", 409));

            assertThatThrownBy(() -> bookingApplicationService.createBooking(cafeId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("overlapping booking")
                    .extracting(ex -> ((BusinessException) ex).getStatus())
                    .isEqualTo(409);
        }
    }

    @Nested
    @DisplayName("Update Booking Tests")
    class UpdateBookingTests {

        @Test
        @DisplayName("Should update booking successfully")
        void shouldUpdateBookingSuccessfully() {
            Instant newStart = bookingStart.plus(30, ChronoUnit.MINUTES);
            Instant newEnd = bookingEnd.plus(30, ChronoUnit.MINUTES);
            UpdateBookingRequest request = new UpdateBookingRequest("Jane Doe", "+9876543210", 6, newStart, newEnd);
            TableBooking existingBooking = new TableBooking(bookingId, cafeId, tableId, "John Doe", "+1234567890", 4, bookingStart, bookingEnd, BookingStatus.CONFIRMED);

            when(tableBookingRepository.findByIdAndCafeId(bookingId, cafeId)).thenReturn(Optional.of(existingBooking));
            doNothing().when(bookingDomainService).checkForOverlappingBookings(cafeId, tableId, newStart, newEnd, bookingId);
            when(tableBookingRepository.save(any(TableBooking.class))).thenAnswer(i -> i.getArgument(0));

            BookingResponse response = bookingApplicationService.updateBooking(cafeId, bookingId, request);

            assertThat(response).isNotNull();
            assertThat(response.customerName()).isEqualTo("Jane Doe");
            assertThat(response.partySize()).isEqualTo(6);
            assertThat(response.bookingStart()).isEqualTo(newStart);
        }

        @Test
        @DisplayName("Should throw 404 when booking to update is not found")
        void shouldThrow404WhenBookingNotFoundForUpdate() {
            UpdateBookingRequest request = new UpdateBookingRequest("Jane Doe", "+9876543210", 6, bookingStart, bookingEnd);

            when(tableBookingRepository.findByIdAndCafeId(bookingId, cafeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingApplicationService.updateBooking(cafeId, bookingId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getStatus())
                    .isEqualTo(404);
        }

        @Test
        @DisplayName("Should throw 409 when updated booking interval overlaps")
        void shouldThrow409WhenUpdatedIntervalOverlaps() {
            UpdateBookingRequest request = new UpdateBookingRequest("Jane Doe", "+9876543210", 6, bookingStart, bookingEnd);
            TableBooking existingBooking = new TableBooking(bookingId, cafeId, tableId, "John Doe", "+1234567890", 4, bookingStart, bookingEnd, BookingStatus.CONFIRMED);

            when(tableBookingRepository.findByIdAndCafeId(bookingId, cafeId)).thenReturn(Optional.of(existingBooking));
            doThrow(new BusinessException("BOOKING_OVERLAP", "Overlapping booking", 409))
                    .when(bookingDomainService).checkForOverlappingBookings(cafeId, tableId, bookingStart, bookingEnd, bookingId);

            assertThatThrownBy(() -> bookingApplicationService.updateBooking(cafeId, bookingId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getStatus())
                    .isEqualTo(409);
        }
    }

    @Nested
    @DisplayName("Cancel Booking Tests")
    class CancelBookingTests {

        @Test
        @DisplayName("Should cancel booking successfully")
        void shouldCancelBookingSuccessfully() {
            TableBooking cancelledBooking = new TableBooking(bookingId, cafeId, tableId, "John Doe", "+1234567890", 4, bookingStart, bookingEnd, BookingStatus.CANCELLED);

            when(bookingDomainService.cancelBooking(bookingId, cafeId)).thenReturn(cancelledBooking);

            BookingResponse response = bookingApplicationService.cancelBooking(cafeId, bookingId);

            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("Convert Booking To Order Tests")
    class ConvertBookingToOrderTests {

        @Test
        @DisplayName("Should convert booking to order with provided posSessionId")
        void shouldConvertBookingToOrderWithExplicitSessionId() {
            TableBooking convertedBooking = new TableBooking(bookingId, cafeId, tableId, "John Doe", "+1234567890", 4, bookingStart, bookingEnd, BookingStatus.CONVERTED_TO_ORDER);
            UUID orderId = UUID.randomUUID();
            OrderResponse orderResponse = new OrderResponse(
                    orderId, cafeId, posSessionId, tableId, "ORD-0001",
                    OrderType.DINE_IN, OrderStatus.DRAFT, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "USD", "Converted from booking for John Doe",
                    now, now, List.of()
            );

            when(bookingDomainService.convertBookingToOrder(bookingId, cafeId)).thenReturn(convertedBooking);
            when(createOrderUseCase.createOrder(eq(cafeId), any(CreateOrderRequest.class))).thenReturn(orderResponse);

            ConvertToOrderResponse response = bookingApplicationService.convertBookingToOrder(cafeId, bookingId, posSessionId);

            assertThat(response).isNotNull();
            assertThat(response.booking().status()).isEqualTo(BookingStatus.CONVERTED_TO_ORDER);
            assertThat(response.order().id()).isEqualTo(orderId);
            assertThat(response.order().diningTableId()).isEqualTo(tableId);

            verify(createOrderUseCase).createOrder(eq(cafeId), any(CreateOrderRequest.class));
        }

        @Test
        @DisplayName("Should convert booking to order finding active open session when posSessionId is null")
        void shouldConvertBookingToOrderFindingOpenSession() {
            TableBooking convertedBooking = new TableBooking(bookingId, cafeId, tableId, "John Doe", "+1234567890", 4, bookingStart, bookingEnd, BookingStatus.CONVERTED_TO_ORDER);
            PosSession openSession = new PosSession(posSessionId, cafeId, UUID.randomUUID(), employeeId, BigDecimal.ZERO);
            UUID orderId = UUID.randomUUID();
            OrderResponse orderResponse = new OrderResponse(
                    orderId, cafeId, posSessionId, tableId, "ORD-0001",
                    OrderType.DINE_IN, OrderStatus.DRAFT, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "USD", "Converted from booking for John Doe",
                    now, now, List.of()
            );

            when(bookingDomainService.convertBookingToOrder(bookingId, cafeId)).thenReturn(convertedBooking);
            when(posSessionRepository.findByCafeId(cafeId)).thenReturn(List.of(openSession));
            when(createOrderUseCase.createOrder(eq(cafeId), any(CreateOrderRequest.class))).thenReturn(orderResponse);

            ConvertToOrderResponse response = bookingApplicationService.convertBookingToOrder(cafeId, bookingId, null);

            assertThat(response).isNotNull();
            assertThat(response.booking().status()).isEqualTo(BookingStatus.CONVERTED_TO_ORDER);
            assertThat(response.order().id()).isEqualTo(orderId);
        }

        @Test
        @DisplayName("Should throw 400 when posSessionId is null and no open session exists")
        void shouldThrow400WhenNoOpenSessionFound() {
            TableBooking convertedBooking = new TableBooking(bookingId, cafeId, tableId, "John Doe", "+1234567890", 4, bookingStart, bookingEnd, BookingStatus.CONVERTED_TO_ORDER);

            when(bookingDomainService.convertBookingToOrder(bookingId, cafeId)).thenReturn(convertedBooking);
            when(posSessionRepository.findByCafeId(cafeId)).thenReturn(List.of());

            assertThatThrownBy(() -> bookingApplicationService.convertBookingToOrder(cafeId, bookingId, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("No open POS session found")
                    .extracting(ex -> ((BusinessException) ex).getStatus())
                    .isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("Get Bookings Tests")
    class GetBookingsTests {

        @Test
        @DisplayName("Should get bookings for cafe")
        void shouldGetBookingsForCafe() {
            TableBooking booking = new TableBooking(bookingId, cafeId, tableId, "John Doe", "+1234567890", 4, bookingStart, bookingEnd, BookingStatus.CONFIRMED);

            when(tableBookingRepository.findByCafeId(cafeId)).thenReturn(List.of(booking));

            List<BookingResponse> responses = bookingApplicationService.getBookings(cafeId, null, null);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).id()).isEqualTo(bookingId);
        }

        @Test
        @DisplayName("Should get booking by ID")
        void shouldGetBookingById() {
            TableBooking booking = new TableBooking(bookingId, cafeId, tableId, "John Doe", "+1234567890", 4, bookingStart, bookingEnd, BookingStatus.CONFIRMED);

            when(tableBookingRepository.findByIdAndCafeId(bookingId, cafeId)).thenReturn(Optional.of(booking));

            BookingResponse response = bookingApplicationService.getBookingById(cafeId, bookingId);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(bookingId);
        }

        @Test
        @DisplayName("Should throw 404 when booking by ID is not found")
        void shouldThrow404WhenBookingByIdNotFound() {
            when(tableBookingRepository.findByIdAndCafeId(bookingId, cafeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingApplicationService.getBookingById(cafeId, bookingId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getStatus())
                    .isEqualTo(404);
        }

        @Test
        @DisplayName("Should expire lapsed CONFIRMED bookings")
        void shouldExpireLapsedBookings() {
            Instant lapsedStart = Instant.now().minus(3, ChronoUnit.HOURS);
            Instant lapsedEnd = lapsedStart.plus(1, ChronoUnit.HOURS);
            TableBooking lapsedBooking = new TableBooking(bookingId, cafeId, tableId, "John Doe", "+1234567890", 4, lapsedStart, lapsedEnd, BookingStatus.CONFIRMED);

            when(tableBookingRepository.findByCafeIdAndStatus(cafeId, BookingStatus.CONFIRMED)).thenReturn(List.of(lapsedBooking));
            when(tableBookingRepository.save(any(TableBooking.class))).thenAnswer(i -> i.getArgument(0));

            int count = bookingApplicationService.expireLapsedBookings(cafeId);

            assertThat(count).isEqualTo(1);
            assertThat(lapsedBooking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
            verify(tableBookingRepository).save(lapsedBooking);
        }
    }
}
