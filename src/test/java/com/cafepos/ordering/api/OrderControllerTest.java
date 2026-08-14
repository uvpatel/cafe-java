package com.cafepos.ordering.api;

import com.cafepos.ordering.application.AssignTableUseCase;
import com.cafepos.ordering.application.CancelOrderUseCase;
import com.cafepos.ordering.application.CreateOrderUseCase;
import com.cafepos.ordering.application.GetOrderUseCase;
import com.cafepos.ordering.application.ManageOrderLinesUseCase;
import com.cafepos.ordering.domain.OrderStatus;
import com.cafepos.ordering.domain.OrderType;
import com.cafepos.shared.error.BusinessException;
import com.cafepos.shared.error.GlobalExceptionHandler;
import com.cafepos.shared.security.UserPrincipal;
import com.cafepos.shared.tenancy.TenantContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CreateOrderUseCase createOrderUseCase;

    @Mock
    private ManageOrderLinesUseCase manageOrderLinesUseCase;

    @Mock
    private AssignTableUseCase assignTableUseCase;

    @Mock
    private CancelOrderUseCase cancelOrderUseCase;

    @Mock
    private GetOrderUseCase getOrderUseCase;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();
    private final UUID lineId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        OrderController controller = new OrderController(
                createOrderUseCase,
                manageOrderLinesUseCase,
                assignTableUseCase,
                cancelOrderUseCase,
                getOrderUseCase
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        TenantContextHolder.setTenantId(cafeId.toString());
        UserPrincipal principal = new UserPrincipal(cafeId, employeeId, "cashier@cafe.com", "CASHIER");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_CASHIER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/v1/orders should create order with 201 CREATED")
    void shouldCreateOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(sessionId, OrderType.DINE_IN, tableId, "Test note", "USD");
        OrderResponse response = new OrderResponse(
                orderId, cafeId, sessionId, tableId, "ORD-20260814-0001",
                OrderType.DINE_IN, OrderStatus.DRAFT, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, "USD", "Test note",
                Instant.now(), Instant.now(), List.of()
        );

        when(createOrderUseCase.createOrder(eq(cafeId), any(CreateOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.orderNumber").value("ORD-20260814-0001"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(createOrderUseCase).createOrder(eq(cafeId), any(CreateOrderRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/orders should list orders with 200 OK")
    void shouldListOrders() throws Exception {
        OrderResponse response = new OrderResponse(
                orderId, cafeId, sessionId, tableId, "ORD-20260814-0001",
                OrderType.DINE_IN, OrderStatus.DRAFT, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, "USD", null,
                Instant.now(), Instant.now(), List.of()
        );

        when(getOrderUseCase.getOrders(cafeId, sessionId, OrderStatus.DRAFT))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/orders")
                        .param("posSessionId", sessionId.toString())
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(orderId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{id} should return order details with 200 OK")
    void shouldGetOrderById() throws Exception {
        OrderResponse response = new OrderResponse(
                orderId, cafeId, sessionId, tableId, "ORD-20260814-0001",
                OrderType.DINE_IN, OrderStatus.DRAFT, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, "USD", null,
                Instant.now(), Instant.now(), List.of()
        );

        when(getOrderUseCase.getOrderById(cafeId, orderId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{id}/lines should add order line with 201 CREATED")
    void shouldAddOrderLine() throws Exception {
        AddOrderLineRequest request = new AddOrderLineRequest(productId, 2);
        OrderLineResponse lineResponse = new OrderLineResponse(
                lineId, orderId, productId, "Latte", new BigDecimal("4.0000"),
                2, new BigDecimal("8.0000"), new BigDecimal("8.0000"), true, Instant.now()
        );
        OrderResponse response = new OrderResponse(
                orderId, cafeId, sessionId, tableId, "ORD-20260814-0001",
                OrderType.DINE_IN, OrderStatus.DRAFT, new BigDecimal("8.0000"), BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("8.0000"), "USD", null,
                Instant.now(), Instant.now(), List.of(lineResponse)
        );

        when(manageOrderLinesUseCase.addOrderLine(eq(cafeId), eq(orderId), any(AddOrderLineRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/orders/{id}/lines", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lines[0].id").value(lineId.toString()))
                .andExpect(jsonPath("$.lines[0].productNameSnapshot").value("Latte"));
    }

    @Test
    @DisplayName("PATCH /api/v1/orders/{id}/lines/{lineId} should update line quantity with 200 OK")
    void shouldUpdateOrderLine() throws Exception {
        UpdateOrderLineRequest request = new UpdateOrderLineRequest(3);
        OrderResponse response = new OrderResponse(
                orderId, cafeId, sessionId, tableId, "ORD-20260814-0001",
                OrderType.DINE_IN, OrderStatus.DRAFT, new BigDecimal("12.0000"), BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("12.0000"), "USD", null,
                Instant.now(), Instant.now(), List.of()
        );

        when(manageOrderLinesUseCase.updateOrderLine(eq(cafeId), eq(orderId), eq(lineId), any(UpdateOrderLineRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/orders/{id}/lines/{lineId}", orderId, lineId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalAmount").value(12.0));
    }

    @Test
    @DisplayName("DELETE /api/v1/orders/{id}/lines/{lineId} should remove line with 200 OK")
    void shouldDeleteOrderLine() throws Exception {
        OrderResponse response = new OrderResponse(
                orderId, cafeId, sessionId, tableId, "ORD-20260814-0001",
                OrderType.DINE_IN, OrderStatus.DRAFT, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, "USD", null,
                Instant.now(), Instant.now(), List.of()
        );

        when(manageOrderLinesUseCase.deleteOrderLine(cafeId, orderId, lineId)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/orders/{id}/lines/{lineId}", orderId, lineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines").isEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/orders/{id}/table should assign table with 200 OK")
    void shouldAssignTable() throws Exception {
        AssignTableRequest request = new AssignTableRequest(tableId);
        OrderResponse response = new OrderResponse(
                orderId, cafeId, sessionId, tableId, "ORD-20260814-0001",
                OrderType.DINE_IN, OrderStatus.DRAFT, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, "USD", null,
                Instant.now(), Instant.now(), List.of()
        );

        when(assignTableUseCase.assignTable(eq(cafeId), eq(orderId), any(AssignTableRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/orders/{id}/table", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diningTableId").value(tableId.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{id}/table when occupied should return 409 Conflict")
    void shouldReturn409WhenTableOccupied() throws Exception {
        AssignTableRequest request = new AssignTableRequest(tableId);

        when(assignTableUseCase.assignTable(eq(cafeId), eq(orderId), any(AssignTableRequest.class)))
                .thenThrow(new BusinessException("TABLE_OCCUPIED", "Table is occupied", 409));

        mockMvc.perform(post("/api/v1/orders/{id}/table", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TABLE_OCCUPIED"));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{id}/cancel should cancel order with 200 OK")
    void shouldCancelOrder() throws Exception {
        OrderResponse response = new OrderResponse(
                orderId, cafeId, sessionId, tableId, "ORD-20260814-0001",
                OrderType.DINE_IN, OrderStatus.CANCELLED, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, "USD", null,
                Instant.now(), Instant.now(), List.of()
        );

        when(cancelOrderUseCase.cancelOrder(cafeId, orderId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
