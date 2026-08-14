package com.cafepos.kitchen.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KitchenTicketItemRepository extends JpaRepository<KitchenTicketItem, UUID> {

    List<KitchenTicketItem> findByTicketId(UUID ticketId);
}
