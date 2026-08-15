package com.cafepos.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {
    List<StockMovement> findByCafeIdAndInventoryItemIdOrderByCreatedAtDesc(UUID cafeId, UUID inventoryItemId);
}
