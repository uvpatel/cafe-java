package com.cafepos.session.application;

import com.cafepos.session.api.CashMovementResponse;
import com.cafepos.session.api.RecordCashMovementRequest;

import java.util.UUID;

public interface RecordCashMovementUseCase {
    CashMovementResponse recordCashMovement(UUID cafeId, UUID sessionId, UUID employeeId, RecordCashMovementRequest request);
}
