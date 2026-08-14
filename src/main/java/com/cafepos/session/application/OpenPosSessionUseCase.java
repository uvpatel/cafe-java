package com.cafepos.session.application;

import com.cafepos.session.api.OpenSessionRequest;
import com.cafepos.session.api.PosSessionResponse;

import java.util.UUID;

public interface OpenPosSessionUseCase {
    PosSessionResponse openSession(UUID cafeId, UUID employeeId, OpenSessionRequest request);
}
