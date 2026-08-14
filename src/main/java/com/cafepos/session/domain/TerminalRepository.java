package com.cafepos.session.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TerminalRepository extends JpaRepository<Terminal, UUID> {

    List<Terminal> findByCafeId(UUID cafeId);

    List<Terminal> findByCafeIdAndActiveTrue(UUID cafeId);

    Optional<Terminal> findByIdAndCafeId(UUID id, UUID cafeId);

    Optional<Terminal> findByCafeIdAndCodeIgnoreCase(UUID cafeId, String code);

    boolean existsByCafeIdAndCodeIgnoreCase(UUID cafeId, String code);
}
