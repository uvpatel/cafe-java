package com.cafepos.session.application;

import com.cafepos.session.api.PosSessionResponse;

import java.util.UUID;

public interface GetCurrentSessionUseCase {
    PosSessionResponse getCurrentSession(UUID cafeId, UUID terminalId, UUID employeeId);
}
