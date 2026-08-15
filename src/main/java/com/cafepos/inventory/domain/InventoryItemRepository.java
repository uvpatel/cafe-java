package com.cafepos.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    List<InventoryItem> findByCafeId(UUID cafeId);
    Optional<InventoryItem> findByCafeIdAndId(UUID cafeId, UUID id);
    Optional<InventoryItem> findByCafeIdAndSku(UUID cafeId, String sku);
    Optional<InventoryItem> findByCafeIdAndProductId(UUID cafeId, UUID productId);
}
