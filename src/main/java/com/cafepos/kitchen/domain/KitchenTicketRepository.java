package com.cafepos.kitchen.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KitchenTicketRepository extends JpaRepository<KitchenTicket, UUID> {

    Optional<KitchenTicket> findByOrderId(UUID orderId);

    Optional<KitchenTicket> findByIdAndCafeId(UUID id, UUID cafeId);

    List<KitchenTicket> findByCafeId(UUID cafeId);

    List<KitchenTicket> findByCafeIdAndStatus(UUID cafeId, KitchenTicketStatus status);
}
