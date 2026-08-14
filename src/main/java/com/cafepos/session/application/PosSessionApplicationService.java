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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PosSessionApplicationService implements OpenPosSessionUseCase, ClosePosSessionUseCase, RecordCashMovementUseCase, GetCurrentSessionUseCase {

    private final PosSessionRepository posSessionRepository;
    private final CashMovementRepository cashMovementRepository;
    private final TerminalRepository terminalRepository;
    private final PosSessionDtoMapper dtoMapper;

    public PosSessionApplicationService(PosSessionRepository posSessionRepository,
                                       CashMovementRepository cashMovementRepository,
                                       TerminalRepository terminalRepository,
                                       PosSessionDtoMapper dtoMapper) {
        this.posSessionRepository = Objects.requireNonNull(posSessionRepository, "posSessionRepository must not be null");
        this.cashMovementRepository = Objects.requireNonNull(cashMovementRepository, "cashMovementRepository must not be null");
        this.terminalRepository = Objects.requireNonNull(terminalRepository, "terminalRepository must not be null");
        this.dtoMapper = Objects.requireNonNull(dtoMapper, "dtoMapper must not be null");
    }

    @Override
    public PosSessionResponse openSession(UUID cafeId, UUID employeeId, OpenSessionRequest request) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(employeeId, "employeeId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        Terminal terminal = terminalRepository.findByIdAndCafeId(request.terminalId(), cafeId)
                .orElseThrow(() -> new BusinessException("TERMINAL_NOT_FOUND", "Terminal not found with id " + request.terminalId(), 404));

        if (!terminal.isActive()) {
            throw new BusinessException("TERMINAL_INACTIVE", "Terminal with id " + request.terminalId() + " is inactive", 400);
        }

        boolean hasOpenSession = posSessionRepository.existsByCafeIdAndTerminalIdAndStatus(cafeId, request.terminalId(), PosSessionStatus.OPEN);
        if (hasOpenSession) {
            throw new BusinessException("SESSION_ALREADY_OPEN", "Terminal already has an active OPEN session", 409);
        }

        try {
            PosSession session = PosSession.openSession(cafeId, request.terminalId(), employeeId, request.openingBalanceAmount(), false);
            PosSession saved = posSessionRepository.save(session);
            return dtoMapper.toPosSessionResponse(saved);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BusinessException("SESSION_VALIDATION_FAILED", ex.getMessage(), 400);
        }
    }

    @Override
    public PosSessionResponse closeSession(UUID cafeId, UUID sessionId, UUID employeeId, CloseSessionRequest request) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(employeeId, "employeeId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        PosSession session = posSessionRepository.findByIdAndCafeId(sessionId, cafeId)
                .orElseThrow(() -> new BusinessException("SESSION_NOT_FOUND", "POS Session not found with id " + sessionId, 404));

        if (session.isClosed()) {
            return dtoMapper.toPosSessionResponse(session);
        }

        List<CashMovement> movements = cashMovementRepository.findByCafeIdAndSessionId(cafeId, sessionId);
        BigDecimal totalCashIn = movements.stream()
                .filter(m -> m.getType() == CashMovementType.CASH_IN)
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCashOut = movements.stream()
                .filter(m -> m.getType() == CashMovementType.CASH_OUT)
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expectedAmount = session.getOpeningBalanceAmount().add(totalCashIn).subtract(totalCashOut);

        try {
            session.closeSession(employeeId, expectedAmount, request.countedAmount());
            PosSession saved = posSessionRepository.save(session);
            return dtoMapper.toPosSessionResponse(saved);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("SESSION_VALIDATION_FAILED", ex.getMessage(), 400);
        }
    }

    @Override
    public CashMovementResponse recordCashMovement(UUID cafeId, UUID sessionId, UUID employeeId, RecordCashMovementRequest request) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(employeeId, "employeeId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        PosSession session = posSessionRepository.findByIdAndCafeId(sessionId, cafeId)
                .orElseThrow(() -> new BusinessException("SESSION_NOT_FOUND", "POS Session not found with id " + sessionId, 404));

        if (session.isClosed()) {
            throw new BusinessException("SESSION_CLOSED", "Cannot record cash movement on a closed session", 400);
        }

        try {
            CashMovement movement = session.recordCashMovement(employeeId, request.type(), request.amount(), request.reason());
            CashMovement saved = cashMovementRepository.save(movement);
            return dtoMapper.toCashMovementResponse(saved);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BusinessException("CASH_MOVEMENT_VALIDATION_FAILED", ex.getMessage(), 400);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PosSessionResponse getCurrentSession(UUID cafeId, UUID terminalId, UUID employeeId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");

        Optional<PosSession> sessionOpt;
        if (terminalId != null) {
            sessionOpt = posSessionRepository.findByCafeIdAndTerminalIdAndStatus(cafeId, terminalId, PosSessionStatus.OPEN);
        } else if (employeeId != null) {
            sessionOpt = posSessionRepository.findByCafeIdAndOpenedByEmployeeIdAndStatus(cafeId, employeeId, PosSessionStatus.OPEN);
        } else {
            sessionOpt = Optional.empty();
        }

        PosSession session = sessionOpt.orElseThrow(() -> new BusinessException("SESSION_NOT_FOUND", "No active OPEN session found", 404));
        return dtoMapper.toPosSessionResponse(session);
    }
}
