package com.cafepos.kitchen.api;

import com.cafepos.kitchen.application.DispatchOrderToKitchenUseCase;
import com.cafepos.kitchen.application.GetKitchenTicketsUseCase;
import com.cafepos.kitchen.application.ManageKitchenTicketUseCase;
import com.cafepos.kitchen.domain.KitchenItemStatus;
import com.cafepos.kitchen.domain.KitchenTicketStatus;
import com.cafepos.shared.error.BusinessException;
import com.cafepos.shared.error.GlobalExceptionHandler;
import com.cafepos.shared.security.UserPrincipal;
import com.cafepos.shared.tenancy.TenantContextHolder;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class KitchenControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DispatchOrderToKitchenUseCase dispatchOrderToKitchenUseCase;

    @Mock
    private ManageKitchenTicketUseCase manageKitchenTicketUseCase;

    @Mock
    private GetKitchenTicketsUseCase getKitchenTicketsUseCase;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID ticketId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        KitchenController controller = new KitchenController(
                dispatchOrderToKitchenUseCase,
                manageKitchenTicketUseCase,
                getKitchenTicketsUseCase
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        TenantContextHolder.setTenantId(cafeId.toString());
        UserPrincipal principal = new UserPrincipal(cafeId, employeeId, "kitchen@cafe.com", "KITCHEN_STAFF");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_KITCHEN_STAFF")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/v1/orders/{id}/kitchen-dispatches should dispatch order to kitchen with 201 CREATED")
    void shouldDispatchOrderToKitchen() throws Exception {
        KitchenTicketItemResponse itemResponse = new KitchenTicketItemResponse(
                itemId, ticketId, UUID.randomUUID(), UUID.randomUUID(), "Espresso", 2, KitchenItemStatus.TO_COOK, Instant.now(), Instant.now()
        );
        DispatchOrderToKitchenResponse response = new DispatchOrderToKitchenResponse(
                ticketId, cafeId, orderId, "ORD-001", KitchenTicketStatus.TO_COOK, Instant.now(), Instant.now(), List.of(itemResponse)
        );

        when(dispatchOrderToKitchenUseCase.dispatchOrderToKitchen(cafeId, orderId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders/{id}/kitchen-dispatches", orderId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketId").value(ticketId.toString()))
                .andExpect(jsonPath("$.orderNumber").value("ORD-001"))
                .andExpect(jsonPath("$.status").value("TO_COOK"))
                .andExpect(jsonPath("$.items[0].productName").value("Espresso"));

        verify(dispatchOrderToKitchenUseCase).dispatchOrderToKitchen(cafeId, orderId);
    }

    @Test
    @DisplayName("GET /api/v1/kitchen/tickets should return active tickets snapshot with 200 OK")
    void shouldGetKitchenTickets() throws Exception {
        KitchenTicketResponse ticketResponse = new KitchenTicketResponse(
                ticketId, cafeId, orderId, "ORD-001", KitchenTicketStatus.TO_COOK, Instant.now(), Instant.now(), List.of()
        );

        when(getKitchenTicketsUseCase.getKitchenTickets(cafeId, KitchenTicketStatus.TO_COOK))
                .thenReturn(List.of(ticketResponse));

        mockMvc.perform(get("/api/v1/kitchen/tickets")
                        .param("status", "TO_COOK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ticketId.toString()))
                .andExpect(jsonPath("$[0].status").value("TO_COOK"));

        verify(getKitchenTicketsUseCase).getKitchenTickets(cafeId, KitchenTicketStatus.TO_COOK);
    }

    @Test
    @DisplayName("POST /api/v1/kitchen/tickets/{id}/start should mark ticket PREPARING with 200 OK")
    void shouldStartPreparingTicket() throws Exception {
        KitchenTicketResponse ticketResponse = new KitchenTicketResponse(
                ticketId, cafeId, orderId, "ORD-001", KitchenTicketStatus.PREPARING, Instant.now(), Instant.now(), List.of()
        );

        when(manageKitchenTicketUseCase.startPreparing(cafeId, ticketId)).thenReturn(ticketResponse);

        mockMvc.perform(post("/api/v1/kitchen/tickets/{id}/start", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId.toString()))
                .andExpect(jsonPath("$.status").value("PREPARING"));

        verify(manageKitchenTicketUseCase).startPreparing(cafeId, ticketId);
    }

    @Test
    @DisplayName("POST /api/v1/kitchen/tickets/{id}/complete should mark ticket COMPLETED with 200 OK")
    void shouldCompleteTicket() throws Exception {
        KitchenTicketResponse ticketResponse = new KitchenTicketResponse(
                ticketId, cafeId, orderId, "ORD-001", KitchenTicketStatus.COMPLETED, Instant.now(), Instant.now(), List.of()
        );

        when(manageKitchenTicketUseCase.completeTicket(cafeId, ticketId)).thenReturn(ticketResponse);

        mockMvc.perform(post("/api/v1/kitchen/tickets/{id}/complete", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(manageKitchenTicketUseCase).completeTicket(cafeId, ticketId);
    }

    @Test
    @DisplayName("POST /api/v1/kitchen/items/{itemId}/complete should complete individual item with 200 OK")
    void shouldCompleteItem() throws Exception {
        KitchenTicketItemResponse itemResponse = new KitchenTicketItemResponse(
                itemId, ticketId, UUID.randomUUID(), UUID.randomUUID(), "Espresso", 2, KitchenItemStatus.COMPLETED, Instant.now(), Instant.now()
        );
        KitchenTicketResponse ticketResponse = new KitchenTicketResponse(
                ticketId, cafeId, orderId, "ORD-001", KitchenTicketStatus.COMPLETED, Instant.now(), Instant.now(), List.of(itemResponse)
        );

        when(manageKitchenTicketUseCase.completeItem(cafeId, itemId)).thenReturn(ticketResponse);

        mockMvc.perform(post("/api/v1/kitchen/items/{itemId}/complete", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(itemId.toString()))
                .andExpect(jsonPath("$.items[0].status").value("COMPLETED"));

        verify(manageKitchenTicketUseCase).completeItem(cafeId, itemId);
    }

    @Test
    @DisplayName("POST /api/v1/kitchen/tickets/{id}/start with non-existent ticket should return 404 Not Found")
    void shouldReturn404WhenTicketNotFound() throws Exception {
        when(manageKitchenTicketUseCase.startPreparing(cafeId, ticketId))
                .thenThrow(new BusinessException("TICKET_NOT_FOUND", "Kitchen ticket not found", 404));

        mockMvc.perform(post("/api/v1/kitchen/tickets/{id}/start", ticketId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
    }
}
