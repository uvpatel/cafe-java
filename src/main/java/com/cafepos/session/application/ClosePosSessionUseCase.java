package com.cafepos.session.application;

import com.cafepos.session.api.CloseSessionRequest;
import com.cafepos.session.api.PosSessionResponse;

import java.util.UUID;

public interface ClosePosSessionUseCase {
    PosSessionResponse closeSession(UUID cafeId, UUID sessionId, UUID employeeId, CloseSessionRequest request);
}
