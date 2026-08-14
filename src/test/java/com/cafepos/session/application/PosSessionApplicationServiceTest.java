package com.cafepos.session.application;

import com.cafepos.session.api.CashMovementResponse;
import com.cafepos.session.api.CloseSessionRequest;
import com.cafepos.session.api.OpenSessionRequest;
import com.cafepos.session.api.PosSessionDtoMapper;
import com.cafepos.session.api.PosSessionResponse;
import com.cafepos.session.api.RecordCashMovementRequest;
import com.cafepos.session.domain.CashMovement;
import com.cafepos.session.domain.CashMovementRepository;
import com.cafepos.session.domain.CashMovementType;
import com.cafepos.session.domain.PosSession;
import com.cafepos.session.domain.PosSessionRepository;
import com.cafepos.session.domain.PosSessionStatus;
import com.cafepos.session.domain.Terminal;
import com.cafepos.session.domain.TerminalRepository;
import com.cafepos.shared.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PosSessionApplicationServiceTest {

    @Mock
    private PosSessionRepository posSessionRepository;

    @Mock
    private CashMovementRepository cashMovementRepository;

    @Mock
    private TerminalRepository terminalRepository;

    @Spy
    private PosSessionDtoMapper dtoMapper = new PosSessionDtoMapper();

    @InjectMocks
    private PosSessionApplicationService applicationService;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID terminalId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();

    private Terminal activeTerminal;
    private Terminal inactiveTerminal;

    @BeforeEach
    void setUp() {
        activeTerminal = new Terminal(terminalId, cafeId, "Main Counter", "TERM-01", true);
        inactiveTerminal = new Terminal(UUID.randomUUID(), cafeId, "Secondary Counter", "TERM-02", false);
    }

    @Test
    @DisplayName("openSession should successfully create open session when terminal is active and no session exists")
    void openSession_success() {
        OpenSessionRequest request = new OpenSessionRequest(terminalId, new BigDecimal("100.00"));

        when(terminalRepository.findByIdAndCafeId(terminalId, cafeId)).thenReturn(Optional.of(activeTerminal));
        when(posSessionRepository.existsByCafeIdAndTerminalIdAndStatus(cafeId, terminalId, PosSessionStatus.OPEN)).thenReturn(false);
        when(posSessionRepository.save(any(PosSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PosSessionResponse response = applicationService.openSession(cafeId, employeeId, request);

        assertThat(response).isNotNull();
        assertThat(response.cafeId()).isEqualTo(cafeId);
        assertThat(response.terminalId()).isEqualTo(terminalId);
        assertThat(response.openedByEmployeeId()).isEqualTo(employeeId);
        assertThat(response.status()).isEqualTo(PosSessionStatus.OPEN);
        assertThat(response.openingBalanceAmount()).isEqualByComparingTo("100.00");

        verify(posSessionRepository).save(any(PosSession.class));
    }

    @Test
    @DisplayName("openSession should throw TERMINAL_NOT_FOUND when terminal does not exist")
    void openSession_terminalNotFound() {
        OpenSessionRequest request = new OpenSessionRequest(terminalId, new BigDecimal("100.00"));

        when(terminalRepository.findByIdAndCafeId(terminalId, cafeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.openSession(cafeId, employeeId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Terminal not found")
                .extracting("code")
                .isEqualTo("TERMINAL_NOT_FOUND");

        verify(posSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("openSession should throw TERMINAL_INACTIVE when terminal is not active")
    void openSession_terminalInactive() {
        OpenSessionRequest request = new OpenSessionRequest(inactiveTerminal.getId(), new BigDecimal("100.00"));

        when(terminalRepository.findByIdAndCafeId(inactiveTerminal.getId(), cafeId)).thenReturn(Optional.of(inactiveTerminal));

        assertThatThrownBy(() -> applicationService.openSession(cafeId, employeeId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("is inactive")
                .extracting("code")
                .isEqualTo("TERMINAL_INACTIVE");

        verify(posSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("openSession should throw SESSION_ALREADY_OPEN when active open session exists")
    void openSession_alreadyOpen() {
        OpenSessionRequest request = new OpenSessionRequest(terminalId, new BigDecimal("100.00"));

        when(terminalRepository.findByIdAndCafeId(terminalId, cafeId)).thenReturn(Optional.of(activeTerminal));
        when(posSessionRepository.existsByCafeIdAndTerminalIdAndStatus(cafeId, terminalId, PosSessionStatus.OPEN)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.openSession(cafeId, employeeId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Terminal already has an active OPEN session")
                .extracting("code")
                .isEqualTo("SESSION_ALREADY_OPEN");

        verify(posSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("closeSession should calculate expected balance, variance, and lock session")
    void closeSession_success() {
        PosSession session = new PosSession(sessionId, cafeId, terminalId, employeeId, new BigDecimal("100.00"));
        CloseSessionRequest request = new CloseSessionRequest(new BigDecimal("220.00"));

        CashMovement cashIn = new CashMovement(cafeId, sessionId, employeeId, CashMovementType.CASH_IN, new BigDecimal("150.00"), "Petty cash float");
        CashMovement cashOut = new CashMovement(cafeId, sessionId, employeeId, CashMovementType.CASH_OUT, new BigDecimal("30.00"), "Supplier cash payout");

        when(posSessionRepository.findByIdAndCafeId(sessionId, cafeId)).thenReturn(Optional.of(session));
        when(cashMovementRepository.findByCafeIdAndSessionId(cafeId, sessionId)).thenReturn(List.of(cashIn, cashOut));
        when(posSessionRepository.save(any(PosSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PosSessionResponse response = applicationService.closeSession(cafeId, sessionId, employeeId, request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(PosSessionStatus.CLOSED);
        assertThat(response.closedByEmployeeId()).isEqualTo(employeeId);

        // Expected balance = 100.00 + 150.00 - 30.00 = 220.00
        assertThat(response.closingExpectedAmount()).isEqualByComparingTo("220.00");
        assertThat(response.closingCountedAmount()).isEqualByComparingTo("220.00");
        assertThat(response.closingVarianceAmount()).isEqualByComparingTo("0.00");

        verify(posSessionRepository).save(session);
    }

    @Test
    @DisplayName("closeSession should return existing closed session without error when session is already closed")
    void closeSession_alreadyClosed() {
        PosSession session = new PosSession(sessionId, cafeId, terminalId, employeeId, new BigDecimal("100.00"));
        session.closeSession(employeeId, new BigDecimal("100.00"), new BigDecimal("100.00"));

        CloseSessionRequest request = new CloseSessionRequest(new BigDecimal("100.00"));

        when(posSessionRepository.findByIdAndCafeId(sessionId, cafeId)).thenReturn(Optional.of(session));

        PosSessionResponse response = applicationService.closeSession(cafeId, sessionId, employeeId, request);

        assertThat(response.status()).isEqualTo(PosSessionStatus.CLOSED);
        verify(posSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordCashMovement should record cash movement for open session")
    void recordCashMovement_success() {
        PosSession session = new PosSession(sessionId, cafeId, terminalId, employeeId, new BigDecimal("100.00"));
        RecordCashMovementRequest request = new RecordCashMovementRequest(CashMovementType.CASH_IN, new BigDecimal("50.00"), "Add change float");

        when(posSessionRepository.findByIdAndCafeId(sessionId, cafeId)).thenReturn(Optional.of(session));
        when(cashMovementRepository.save(any(CashMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CashMovementResponse response = applicationService.recordCashMovement(cafeId, sessionId, employeeId, request);

        assertThat(response).isNotNull();
        assertThat(response.sessionId()).isEqualTo(sessionId);
        assertThat(response.type()).isEqualTo(CashMovementType.CASH_IN);
        assertThat(response.amount()).isEqualByComparingTo("50.00");
        assertThat(response.reason()).isEqualTo("Add change float");

        verify(cashMovementRepository).save(any(CashMovement.class));
    }

    @Test
    @DisplayName("recordCashMovement should throw SESSION_CLOSED when session is closed")
    void recordCashMovement_sessionClosed() {
        PosSession session = new PosSession(sessionId, cafeId, terminalId, employeeId, new BigDecimal("100.00"));
        session.closeSession(employeeId, new BigDecimal("100.00"), new BigDecimal("100.00"));

        RecordCashMovementRequest request = new RecordCashMovementRequest(CashMovementType.CASH_IN, new BigDecimal("50.00"), "Add change float");

        when(posSessionRepository.findByIdAndCafeId(sessionId, cafeId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> applicationService.recordCashMovement(cafeId, sessionId, employeeId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot record cash movement on a closed session")
                .extracting("code")
                .isEqualTo("SESSION_CLOSED");

        verify(cashMovementRepository, never()).save(any());
    }

    @Test
    @DisplayName("getCurrentSession should retrieve current open session by terminalId")
    void getCurrentSession_byTerminalId() {
        PosSession session = new PosSession(sessionId, cafeId, terminalId, employeeId, new BigDecimal("100.00"));

        when(posSessionRepository.findByCafeIdAndTerminalIdAndStatus(cafeId, terminalId, PosSessionStatus.OPEN))
                .thenReturn(Optional.of(session));

        PosSessionResponse response = applicationService.getCurrentSession(cafeId, terminalId, employeeId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(sessionId);
        assertThat(response.terminalId()).isEqualTo(terminalId);
    }

    @Test
    @DisplayName("getCurrentSession should retrieve current open session by employeeId when terminalId is null")
    void getCurrentSession_byEmployeeId() {
        PosSession session = new PosSession(sessionId, cafeId, terminalId, employeeId, new BigDecimal("100.00"));

        when(posSessionRepository.findByCafeIdAndOpenedByEmployeeIdAndStatus(cafeId, employeeId, PosSessionStatus.OPEN))
                .thenReturn(Optional.of(session));

        PosSessionResponse response = applicationService.getCurrentSession(cafeId, null, employeeId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(sessionId);
        assertThat(response.openedByEmployeeId()).isEqualTo(employeeId);
    }

    @Test
    @DisplayName("getCurrentSession should throw SESSION_NOT_FOUND when no active open session exists")
    void getCurrentSession_notFound() {
        when(posSessionRepository.findByCafeIdAndTerminalIdAndStatus(cafeId, terminalId, PosSessionStatus.OPEN))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getCurrentSession(cafeId, terminalId, employeeId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No active OPEN session found")
                .extracting("code")
                .isEqualTo("SESSION_NOT_FOUND");
    }
}
