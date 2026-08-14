package com.cafepos.kitchen.api;

import com.cafepos.kitchen.application.DispatchOrderToKitchenUseCase;
import com.cafepos.kitchen.application.GetKitchenTicketsUseCase;
import com.cafepos.kitchen.application.ManageKitchenTicketUseCase;
import com.cafepos.kitchen.domain.KitchenTicketStatus;
import com.cafepos.shared.tenancy.TenantContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@PreAuthorize("hasAnyRole('KITCHEN_STAFF', 'CASHIER', 'OWNER', 'ADMIN')")
public class KitchenController {

    private final DispatchOrderToKitchenUseCase dispatchOrderToKitchenUseCase;
    private final ManageKitchenTicketUseCase manageKitchenTicketUseCase;
    private final GetKitchenTicketsUseCase getKitchenTicketsUseCase;

    public KitchenController(DispatchOrderToKitchenUseCase dispatchOrderToKitchenUseCase,
                             ManageKitchenTicketUseCase manageKitchenTicketUseCase,
                             GetKitchenTicketsUseCase getKitchenTicketsUseCase) {
        this.dispatchOrderToKitchenUseCase = Objects.requireNonNull(dispatchOrderToKitchenUseCase, "dispatchOrderToKitchenUseCase must not be null");
        this.manageKitchenTicketUseCase = Objects.requireNonNull(manageKitchenTicketUseCase, "manageKitchenTicketUseCase must not be null");
        this.getKitchenTicketsUseCase = Objects.requireNonNull(getKitchenTicketsUseCase, "getKitchenTicketsUseCase must not be null");
    }

    @PostMapping("/api/v1/orders/{id}/kitchen-dispatches")
    public ResponseEntity<DispatchOrderToKitchenResponse> dispatchOrderToKitchen(@PathVariable("id") UUID orderId) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        DispatchOrderToKitchenResponse response = dispatchOrderToKitchenUseCase.dispatchOrderToKitchen(cafeId, orderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/kitchen/tickets")
    public ResponseEntity<List<KitchenTicketResponse>> getKitchenTickets(
            @RequestParam(name = "status", required = false) KitchenTicketStatus status) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        List<KitchenTicketResponse> responses = getKitchenTicketsUseCase.getKitchenTickets(cafeId, status);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/api/v1/kitchen/tickets/{id}/start")
    public ResponseEntity<KitchenTicketResponse> startPreparing(@PathVariable("id") UUID id) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        KitchenTicketResponse response = manageKitchenTicketUseCase.startPreparing(cafeId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/v1/kitchen/tickets/{id}/complete")
    public ResponseEntity<KitchenTicketResponse> completeTicket(@PathVariable("id") UUID id) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        KitchenTicketResponse response = manageKitchenTicketUseCase.completeTicket(cafeId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/v1/kitchen/items/{itemId}/complete")
    public ResponseEntity<KitchenTicketResponse> completeItem(@PathVariable("itemId") UUID itemId) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        KitchenTicketResponse response = manageKitchenTicketUseCase.completeItem(cafeId, itemId);
        return ResponseEntity.ok(response);
    }
}
