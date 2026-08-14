package com.cafepos.reporting.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExportJobRepository extends JpaRepository<ExportJob, UUID> {

    List<ExportJob> findByCafeIdAndStatus(UUID cafeId, ExportStatus status);

    Optional<ExportJob> findByIdAndCafeId(UUID id, UUID cafeId);
}
