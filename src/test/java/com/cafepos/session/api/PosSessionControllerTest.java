package com.cafepos.session.api;

import com.cafepos.session.application.ClosePosSessionUseCase;
import com.cafepos.session.application.GetCurrentSessionUseCase;
import com.cafepos.session.application.OpenPosSessionUseCase;
import com.cafepos.session.application.RecordCashMovementUseCase;
import com.cafepos.session.domain.CashMovementType;
import com.cafepos.session.domain.PosSessionStatus;
import com.cafepos.shared.error.BusinessException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PosSessionControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private OpenPosSessionUseCase openPosSessionUseCase;

    @Mock
    private ClosePosSessionUseCase closePosSessionUseCase;

    @Mock
    private RecordCashMovementUseCase recordCashMovementUseCase;

    @Mock
    private GetCurrentSessionUseCase getCurrentSessionUseCase;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID terminalId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        PosSessionController controller = new PosSessionController(
                openPosSessionUseCase,
                closePosSessionUseCase,
                recordCashMovementUseCase,
                getCurrentSessionUseCase
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.cafepos.shared.error.GlobalExceptionHandler())
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
    @DisplayName("POST /api/v1/pos-sessions should open new POS session with 201 CREATED")
    void shouldOpenPosSession() throws Exception {
        OpenSessionRequest request = new OpenSessionRequest(terminalId, new BigDecimal("100.00"));
        PosSessionResponse response = new PosSessionResponse(
                sessionId, cafeId, terminalId, employeeId, null,
                PosSessionStatus.OPEN, new BigDecimal("100.00"), null, null, null,
                Instant.now(), null, Instant.now(), Instant.now()
        );

        when(openPosSessionUseCase.openSession(eq(cafeId), eq(employeeId), any(OpenSessionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/pos-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.cafeId").value(cafeId.toString()))
                .andExpect(jsonPath("$.terminalId").value(terminalId.toString()))
                .andExpect(jsonPath("$.openedByEmployeeId").value(employeeId.toString()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.openingBalanceAmount").value(100.00));

        verify(openPosSessionUseCase).openSession(eq(cafeId), eq(employeeId), any(OpenSessionRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/pos-sessions with invalid payload should return 400 Bad Request")
    void shouldReturn400ForInvalidOpenSession() throws Exception {
        OpenSessionRequest request = new OpenSessionRequest(null, new BigDecimal("-10.00"));

        mockMvc.perform(post("/api/v1/pos-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/pos-sessions/current should return active open session")
    void shouldGetCurrentSession() throws Exception {
        PosSessionResponse response = new PosSessionResponse(
                sessionId, cafeId, terminalId, employeeId, null,
                PosSessionStatus.OPEN, new BigDecimal("100.00"), null, null, null,
                Instant.now(), null, Instant.now(), Instant.now()
        );

        when(getCurrentSessionUseCase.getCurrentSession(cafeId, terminalId, employeeId))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/pos-sessions/current")
                        .param("terminalId", terminalId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.status").value("OPEN"));

        verify(getCurrentSessionUseCase).getCurrentSession(cafeId, terminalId, employeeId);
    }

    @Test
    @DisplayName("GET /api/v1/pos-sessions/current when no active session should return 404 Not Found")
    void shouldReturn404WhenNoCurrentSession() throws Exception {
        when(getCurrentSessionUseCase.getCurrentSession(cafeId, terminalId, employeeId))
                .thenThrow(new BusinessException("SESSION_NOT_FOUND", "No active OPEN session found", 404));

        mockMvc.perform(get("/api/v1/pos-sessions/current")
                        .param("terminalId", terminalId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/v1/pos-sessions/{id}/cash-movements should record movement with 201 CREATED")
    void shouldRecordCashMovement() throws Exception {
        RecordCashMovementRequest request = new RecordCashMovementRequest(CashMovementType.CASH_IN, new BigDecimal("50.00"), "Petty cash in");
        UUID movementId = UUID.randomUUID();
        CashMovementResponse response = new CashMovementResponse(
                movementId, cafeId, sessionId, employeeId, CashMovementType.CASH_IN, new BigDecimal("50.00"), "Petty cash in", Instant.now()
        );

        when(recordCashMovementUseCase.recordCashMovement(eq(cafeId), eq(sessionId), eq(employeeId), any(RecordCashMovementRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/pos-sessions/{id}/cash-movements", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(movementId.toString()))
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.type").value("CASH_IN"))
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.reason").value("Petty cash in"));

        verify(recordCashMovementUseCase).recordCashMovement(eq(cafeId), eq(sessionId), eq(employeeId), any(RecordCashMovementRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/pos-sessions/{id}/close should close session shift with 200 OK")
    void shouldCloseSession() throws Exception {
        CloseSessionRequest request = new CloseSessionRequest(new BigDecimal("150.00"));
        PosSessionResponse response = new PosSessionResponse(
                sessionId, cafeId, terminalId, employeeId, employeeId,
                PosSessionStatus.CLOSED, new BigDecimal("100.00"), new BigDecimal("150.00"), new BigDecimal("150.00"), BigDecimal.ZERO,
                Instant.now(), Instant.now(), Instant.now(), Instant.now()
        );

        when(closePosSessionUseCase.closeSession(eq(cafeId), eq(sessionId), eq(employeeId), any(CloseSessionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/pos-sessions/{id}/close", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closingExpectedAmount").value(150.00))
                .andExpect(jsonPath("$.closingCountedAmount").value(150.00))
                .andExpect(jsonPath("$.closingVarianceAmount").value(0));

        verify(closePosSessionUseCase).closeSession(eq(cafeId), eq(sessionId), eq(employeeId), any(CloseSessionRequest.class));
    }
}
