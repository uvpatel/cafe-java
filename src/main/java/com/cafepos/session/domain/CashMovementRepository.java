package com.cafepos.session.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashMovementRepository extends JpaRepository<CashMovement, UUID> {

    List<CashMovement> findByCafeId(UUID cafeId);

    List<CashMovement> findByCafeIdAndSessionId(UUID cafeId, UUID sessionId);

    Optional<CashMovement> findByIdAndCafeId(UUID id, UUID cafeId);
}
