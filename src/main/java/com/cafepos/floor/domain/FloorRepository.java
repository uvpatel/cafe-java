package com.cafepos.floor.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FloorRepository extends JpaRepository<Floor, UUID> {

    List<Floor> findByCafeId(UUID cafeId);

    List<Floor> findByCafeIdOrderBySortOrderAsc(UUID cafeId);

    Optional<Floor> findByIdAndCafeId(UUID id, UUID cafeId);

    boolean existsByCafeIdAndNameIgnoreCase(UUID cafeId, String name);
}
