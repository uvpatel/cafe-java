package com.cafepos.booking.api;

import com.cafepos.booking.application.CancelBookingUseCase;
import com.cafepos.booking.application.ConvertBookingToOrderUseCase;
import com.cafepos.booking.application.CreateBookingUseCase;
import com.cafepos.booking.application.GetBookingsUseCase;
import com.cafepos.booking.application.UpdateBookingUseCase;
import com.cafepos.booking.domain.BookingStatus;
import com.cafepos.shared.tenancy.TenantContextHolder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN')")
public class TableBookingController {

    private final CreateBookingUseCase createBookingUseCase;
    private final UpdateBookingUseCase updateBookingUseCase;
    private final CancelBookingUseCase cancelBookingUseCase;
    private final ConvertBookingToOrderUseCase convertBookingToOrderUseCase;
    private final GetBookingsUseCase getBookingsUseCase;

    public TableBookingController(CreateBookingUseCase createBookingUseCase,
                                  UpdateBookingUseCase updateBookingUseCase,
                                  CancelBookingUseCase cancelBookingUseCase,
                                  ConvertBookingToOrderUseCase convertBookingToOrderUseCase,
                                  GetBookingsUseCase getBookingsUseCase) {
        this.createBookingUseCase = Objects.requireNonNull(createBookingUseCase, "createBookingUseCase must not be null");
        this.updateBookingUseCase = Objects.requireNonNull(updateBookingUseCase, "updateBookingUseCase must not be null");
        this.cancelBookingUseCase = Objects.requireNonNull(cancelBookingUseCase, "cancelBookingUseCase must not be null");
        this.convertBookingToOrderUseCase = Objects.requireNonNull(convertBookingToOrderUseCase, "convertBookingToOrderUseCase must not be null");
        this.getBookingsUseCase = Objects.requireNonNull(getBookingsUseCase, "getBookingsUseCase must not be null");
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getBookings(
            @RequestParam(name = "diningTableId", required = false) UUID diningTableId,
            @RequestParam(name = "status", required = false) BookingStatus status) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        List<BookingResponse> responses = getBookingsUseCase.getBookings(cafeId, diningTableId, status);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable("id") UUID id) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        BookingResponse response = getBookingsUseCase.getBookingById(cafeId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        BookingResponse response = createBookingUseCase.createBooking(cafeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BookingResponse> updateBooking(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateBookingRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        BookingResponse response = updateBookingUseCase.updateBooking(cafeId, id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable("id") UUID id) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        BookingResponse response = cancelBookingUseCase.cancelBooking(cafeId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/convert-to-order")
    public ResponseEntity<ConvertToOrderResponse> convertToOrder(
            @PathVariable("id") UUID id,
            @RequestParam(name = "posSessionId", required = false) UUID posSessionId) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        ConvertToOrderResponse response = convertBookingToOrderUseCase.convertBookingToOrder(cafeId, id, posSessionId);
        return ResponseEntity.ok(response);
    }
}
