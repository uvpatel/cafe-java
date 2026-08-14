package com.cafepos.session.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PosSessionRepository extends JpaRepository<PosSession, UUID> {

    List<PosSession> findByCafeId(UUID cafeId);

    List<PosSession> findByCafeIdAndTerminalId(UUID cafeId, UUID terminalId);

    Optional<PosSession> findByIdAndCafeId(UUID id, UUID cafeId);

    Optional<PosSession> findByCafeIdAndTerminalIdAndStatus(UUID cafeId, UUID terminalId, PosSessionStatus status);

    Optional<PosSession> findByCafeIdAndOpenedByEmployeeIdAndStatus(UUID cafeId, UUID openedByEmployeeId, PosSessionStatus status);

    boolean existsByCafeIdAndTerminalIdAndStatus(UUID cafeId, UUID terminalId, PosSessionStatus status);
}
