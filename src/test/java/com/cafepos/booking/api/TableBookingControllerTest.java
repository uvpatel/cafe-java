package com.cafepos.booking.api;

import com.cafepos.booking.application.CancelBookingUseCase;
import com.cafepos.booking.application.ConvertBookingToOrderUseCase;
import com.cafepos.booking.application.CreateBookingUseCase;
import com.cafepos.booking.application.GetBookingsUseCase;
import com.cafepos.booking.application.UpdateBookingUseCase;
import com.cafepos.booking.domain.BookingStatus;
import com.cafepos.ordering.api.OrderResponse;
import com.cafepos.ordering.domain.OrderStatus;
import com.cafepos.ordering.domain.OrderType;
import com.cafepos.shared.error.BusinessException;
import com.cafepos.shared.error.GlobalExceptionHandler;
import com.cafepos.shared.security.UserPrincipal;
import com.cafepos.shared.tenancy.TenantContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TableBookingControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private CreateBookingUseCase createBookingUseCase;

    @Mock
    private UpdateBookingUseCase updateBookingUseCase;

    @Mock
    private CancelBookingUseCase cancelBookingUseCase;

    @Mock
    private ConvertBookingToOrderUseCase convertBookingToOrderUseCase;

    @Mock
    private GetBookingsUseCase getBookingsUseCase;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();
    private final UUID diningTableId = UUID.randomUUID();
    private final UUID posSessionId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    private Instant now;
    private Instant bookingStart;
    private Instant bookingEnd;

    @BeforeEach
    void setUp() {
        TableBookingController controller = new TableBookingController(
                createBookingUseCase,
                updateBookingUseCase,
                cancelBookingUseCase,
                convertBookingToOrderUseCase,
                getBookingsUseCase
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        TenantContextHolder.setTenantId(cafeId.toString());
        UserPrincipal principal = new UserPrincipal(cafeId, employeeId, "cashier@cafe.com", "CASHIER");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_CASHIER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        bookingStart = now.plus(1, ChronoUnit.HOURS);
        bookingEnd = now.plus(2, ChronoUnit.HOURS);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/bookings should list bookings with 200 OK")
    void shouldListBookings() throws Exception {
        BookingResponse response = new BookingResponse(
                bookingId, cafeId, diningTableId, "John Doe", "+1234567890", 4,
                bookingStart, bookingEnd, BookingStatus.CONFIRMED, now, now
        );

        when(getBookingsUseCase.getBookings(cafeId, diningTableId, BookingStatus.CONFIRMED))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/bookings")
                        .param("diningTableId", diningTableId.toString())
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(bookingId.toString()))
                .andExpect(jsonPath("$[0].customerName").value("John Doe"))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("GET /api/v1/bookings/{id} should return booking details with 200 OK")
    void shouldGetBookingById() throws Exception {
        BookingResponse response = new BookingResponse(
                bookingId, cafeId, diningTableId, "John Doe", "+1234567890", 4,
                bookingStart, bookingEnd, BookingStatus.CONFIRMED, now, now
        );

        when(getBookingsUseCase.getBookingById(cafeId, bookingId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/bookings/{id}", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId.toString()))
                .andExpect(jsonPath("$.customerName").value("John Doe"));
    }

    @Test
    @DisplayName("POST /api/v1/bookings should create booking with 201 CREATED")
    void shouldCreateBooking() throws Exception {
        CreateBookingRequest request = new CreateBookingRequest(
                diningTableId, "John Doe", "+1234567890", 4, bookingStart, bookingEnd
        );
        BookingResponse response = new BookingResponse(
                bookingId, cafeId, diningTableId, "John Doe", "+1234567890", 4,
                bookingStart, bookingEnd, BookingStatus.CONFIRMED, now, now
        );

        when(createBookingUseCase.createBooking(eq(cafeId), any(CreateBookingRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(bookingId.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(createBookingUseCase).createBooking(eq(cafeId), any(CreateBookingRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/bookings when overlap occurs should return 409 Conflict")
    void shouldReturn409WhenBookingOverlaps() throws Exception {
        CreateBookingRequest request = new CreateBookingRequest(
                diningTableId, "John Doe", "+1234567890", 4, bookingStart, bookingEnd
        );

        when(createBookingUseCase.createBooking(eq(cafeId), any(CreateBookingRequest.class)))
                .thenThrow(new BusinessException("BOOKING_OVERLAP", "Dining table has overlapping booking", 409));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOKING_OVERLAP"));
    }

    @Test
    @DisplayName("PATCH /api/v1/bookings/{id} should update booking with 200 OK")
    void shouldUpdateBooking() throws Exception {
        UpdateBookingRequest request = new UpdateBookingRequest(
                "Jane Doe", "+9876543210", 6, bookingStart, bookingEnd
        );
        BookingResponse response = new BookingResponse(
                bookingId, cafeId, diningTableId, "Jane Doe", "+9876543210", 6,
                bookingStart, bookingEnd, BookingStatus.CONFIRMED, now, now
        );

        when(updateBookingUseCase.updateBooking(eq(cafeId), eq(bookingId), any(UpdateBookingRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/bookings/{id}", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Jane Doe"))
                .andExpect(jsonPath("$.partySize").value(6));
    }

    @Test
    @DisplayName("POST /api/v1/bookings/{id}/cancel should cancel booking with 200 OK")
    void shouldCancelBooking() throws Exception {
        BookingResponse response = new BookingResponse(
                bookingId, cafeId, diningTableId, "John Doe", "+1234567890", 4,
                bookingStart, bookingEnd, BookingStatus.CANCELLED, now, now
        );

        when(cancelBookingUseCase.cancelBooking(cafeId, bookingId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/bookings/{id}/cancel", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("POST /api/v1/bookings/{id}/convert-to-order should convert booking to order with 200 OK")
    void shouldConvertBookingToOrder() throws Exception {
        BookingResponse bookingResponse = new BookingResponse(
                bookingId, cafeId, diningTableId, "John Doe", "+1234567890", 4,
                bookingStart, bookingEnd, BookingStatus.CONVERTED_TO_ORDER, now, now
        );
        OrderResponse orderResponse = new OrderResponse(
                orderId, cafeId, posSessionId, diningTableId, "ORD-20260815-0001",
                OrderType.DINE_IN, OrderStatus.DRAFT, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, "USD", "Converted from booking for John Doe",
                now, now, List.of()
        );
        ConvertToOrderResponse response = new ConvertToOrderResponse(bookingResponse, orderResponse);

        when(convertBookingToOrderUseCase.convertBookingToOrder(cafeId, bookingId, posSessionId))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/bookings/{id}/convert-to-order", bookingId)
                        .param("posSessionId", posSessionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.booking.status").value("CONVERTED_TO_ORDER"))
                .andExpect(jsonPath("$.order.id").value(orderId.toString()))
                .andExpect(jsonPath("$.order.diningTableId").value(diningTableId.toString()));
    }
}
