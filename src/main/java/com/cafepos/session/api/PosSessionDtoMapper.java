package com.cafepos.session.api;

import com.cafepos.session.domain.CashMovement;
import com.cafepos.session.domain.PosSession;
import org.springframework.stereotype.Component;

@Component
public class PosSessionDtoMapper {

    public PosSessionResponse toPosSessionResponse(PosSession session) {
        if (session == null) {
            return null;
        }
        return new PosSessionResponse(
                session.getId(),
                session.getCafeId(),
                session.getTerminalId(),
                session.getOpenedByEmployeeId(),
                session.getClosedByEmployeeId(),
                session.getStatus(),
                session.getOpeningBalanceAmount(),
                session.getClosingExpectedAmount(),
                session.getClosingCountedAmount(),
                session.getClosingVarianceAmount(),
                session.getOpenedAt(),
                session.getClosedAt(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    public CashMovementResponse toCashMovementResponse(CashMovement cashMovement) {
        if (cashMovement == null) {
            return null;
        }
        return new CashMovementResponse(
                cashMovement.getId(),
                cashMovement.getCafeId(),
                cashMovement.getSessionId(),
                cashMovement.getEmployeeId(),
                cashMovement.getType(),
                cashMovement.getAmount(),
                cashMovement.getReason(),
                cashMovement.getCreatedAt()
        );
    }
}
