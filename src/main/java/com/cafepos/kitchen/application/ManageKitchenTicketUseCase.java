package com.cafepos.kitchen.application;

import com.cafepos.kitchen.api.KitchenTicketResponse;

import java.util.UUID;

public interface ManageKitchenTicketUseCase {
    KitchenTicketResponse startPreparing(UUID cafeId, UUID ticketId);
    KitchenTicketResponse completeTicket(UUID cafeId, UUID ticketId);
    KitchenTicketResponse completeItem(UUID cafeId, UUID itemId);
}
