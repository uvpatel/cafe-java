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
import com.cafepos.floor.domain.DiningTableRepository;
import com.cafepos.ordering.api.CreateOrderRequest;
import com.cafepos.ordering.api.OrderResponse;
import com.cafepos.ordering.application.CreateOrderUseCase;
import com.cafepos.ordering.domain.OrderType;
import com.cafepos.session.domain.PosSession;
import com.cafepos.session.domain.PosSessionRepository;
import com.cafepos.shared.error.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class BookingApplicationService implements CreateBookingUseCase, UpdateBookingUseCase, CancelBookingUseCase, ConvertBookingToOrderUseCase, GetBookingsUseCase {

    private final BookingDomainService bookingDomainService;
    private final TableBookingRepository tableBookingRepository;
    private final DiningTableRepository diningTableRepository;
    private final CreateOrderUseCase createOrderUseCase;
    private final PosSessionRepository posSessionRepository;
    private final BookingDtoMapper dtoMapper;

    public BookingApplicationService(BookingDomainService bookingDomainService,
                                      TableBookingRepository tableBookingRepository,
                                      DiningTableRepository diningTableRepository,
                                      CreateOrderUseCase createOrderUseCase,
                                      PosSessionRepository posSessionRepository,
                                      BookingDtoMapper dtoMapper) {
        this.bookingDomainService = Objects.requireNonNull(bookingDomainService, "bookingDomainService must not be null");
        this.tableBookingRepository = Objects.requireNonNull(tableBookingRepository, "tableBookingRepository must not be null");
        this.diningTableRepository = Objects.requireNonNull(diningTableRepository, "diningTableRepository must not be null");
        this.createOrderUseCase = Objects.requireNonNull(createOrderUseCase, "createOrderUseCase must not be null");
        this.posSessionRepository = Objects.requireNonNull(posSessionRepository, "posSessionRepository must not be null");
        this.dtoMapper = Objects.requireNonNull(dtoMapper, "dtoMapper must not be null");
    }

    @Override
    public BookingResponse createBooking(UUID cafeId, CreateBookingRequest request) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        diningTableRepository.findByIdAndCafeId(request.diningTableId(), cafeId)
                .orElseThrow(() -> new BusinessException("TABLE_NOT_FOUND", "Dining table not found with id " + request.diningTableId(), 404));

        TableBooking booking = bookingDomainService.createBooking(
                cafeId,
                request.diningTableId(),
                request.customerName(),
                request.customerPhone(),
                request.partySize(),
                request.bookingStart(),
                request.bookingEnd()
        );

        return dtoMapper.toBookingResponse(booking);
    }

    @Override
    public BookingResponse updateBooking(UUID cafeId, UUID bookingId, UpdateBookingRequest request) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(bookingId, "bookingId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        TableBooking booking = tableBookingRepository.findByIdAndCafeId(bookingId, cafeId)
                .orElseThrow(() -> new BusinessException("BOOKING_NOT_FOUND", "Booking not found with id " + bookingId, 404));

        bookingDomainService.checkForOverlappingBookings(
                cafeId,
                booking.getDiningTableId(),
                request.bookingStart(),
                request.bookingEnd(),
                bookingId
        );

        booking.updateDetails(
                request.customerName(),
                request.customerPhone(),
                request.partySize(),
                request.bookingStart(),
                request.bookingEnd()
        );

        TableBooking saved = tableBookingRepository.save(booking);
        return dtoMapper.toBookingResponse(saved);
    }

    @Override
    public BookingResponse cancelBooking(UUID cafeId, UUID bookingId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(bookingId, "bookingId must not be null");

        TableBooking cancelled = bookingDomainService.cancelBooking(bookingId, cafeId);
        return dtoMapper.toBookingResponse(cancelled);
    }

    @Override
    public ConvertToOrderResponse convertBookingToOrder(UUID cafeId, UUID bookingId, UUID posSessionId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(bookingId, "bookingId must not be null");

        TableBooking booking = bookingDomainService.convertBookingToOrder(bookingId, cafeId);

        UUID targetSessionId = posSessionId;
        if (targetSessionId == null) {
            PosSession openSession = posSessionRepository.findByCafeId(cafeId).stream()
                    .filter(PosSession::isOpen)
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("SESSION_NOT_FOUND", "No open POS session found for cafe", 400));
            targetSessionId = openSession.getId();
        }

        CreateOrderRequest orderRequest = new CreateOrderRequest(
                targetSessionId,
                OrderType.DINE_IN,
                booking.getDiningTableId(),
                "Converted from booking for " + booking.getCustomerName(),
                null
        );

        OrderResponse orderResponse = createOrderUseCase.createOrder(cafeId, orderRequest);

        return new ConvertToOrderResponse(dtoMapper.toBookingResponse(booking), orderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookings(UUID cafeId, UUID diningTableId, BookingStatus status) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");

        List<TableBooking> bookings;
        if (diningTableId != null && status != null) {
            bookings = tableBookingRepository.findByCafeIdAndDiningTableId(cafeId, diningTableId).stream()
                    .filter(b -> b.getStatus() == status)
                    .toList();
        } else if (diningTableId != null) {
            bookings = tableBookingRepository.findByCafeIdAndDiningTableId(cafeId, diningTableId);
        } else if (status != null) {
            bookings = tableBookingRepository.findByCafeIdAndStatus(cafeId, status);
        } else {
            bookings = tableBookingRepository.findByCafeId(cafeId);
        }

        return bookings.stream()
                .map(dtoMapper::toBookingResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(UUID cafeId, UUID bookingId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(bookingId, "bookingId must not be null");

        TableBooking booking = tableBookingRepository.findByIdAndCafeId(bookingId, cafeId)
                .orElseThrow(() -> new BusinessException("BOOKING_NOT_FOUND", "Booking not found with id " + bookingId, 404));

        return dtoMapper.toBookingResponse(booking);
    }

    public int expireLapsedBookings(UUID cafeId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        List<TableBooking> confirmedBookings = tableBookingRepository.findByCafeIdAndStatus(cafeId, BookingStatus.CONFIRMED);
        java.time.Instant now = java.time.Instant.now();
        int expiredCount = 0;
        for (TableBooking booking : confirmedBookings) {
            if (booking.getBookingEnd().isBefore(now)) {
                booking.expire();
                tableBookingRepository.save(booking);
                expiredCount++;
            }
        }
        return expiredCount;
    }
}
