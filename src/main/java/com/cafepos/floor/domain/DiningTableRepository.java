package com.cafepos.floor.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiningTableRepository extends JpaRepository<DiningTable, UUID> {

    List<DiningTable> findByCafeId(UUID cafeId);

    List<DiningTable> findByCafeIdAndFloorId(UUID cafeId, UUID floorId);

    List<DiningTable> findByCafeIdAndFloorIdAndActiveTrue(UUID cafeId, UUID floorId);

    Optional<DiningTable> findByIdAndCafeId(UUID id, UUID cafeId);

    List<DiningTable> findByCafeIdAndMergedIntoTableId(UUID cafeId, UUID mergedIntoTableId);

    boolean existsByCafeIdAndFloorIdAndTableNumberIgnoreCase(UUID cafeId, UUID floorId, String tableNumber);
}
