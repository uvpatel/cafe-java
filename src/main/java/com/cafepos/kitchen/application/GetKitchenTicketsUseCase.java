package com.cafepos.kitchen.application;

import com.cafepos.kitchen.api.KitchenTicketResponse;
import com.cafepos.kitchen.domain.KitchenTicketStatus;

import java.util.List;
import java.util.UUID;

public interface GetKitchenTicketsUseCase {
    List<KitchenTicketResponse> getKitchenTickets(UUID cafeId, KitchenTicketStatus status);
    KitchenTicketResponse getKitchenTicketById(UUID cafeId, UUID ticketId);
}
