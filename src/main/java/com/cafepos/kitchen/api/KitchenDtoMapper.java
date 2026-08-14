package com.cafepos.kitchen.api;

import com.cafepos.kitchen.domain.KitchenTicket;
import com.cafepos.kitchen.domain.KitchenTicketItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class KitchenDtoMapper {

    public KitchenTicketResponse toTicketResponse(KitchenTicket ticket) {
        Objects.requireNonNull(ticket, "ticket must not be null");
        List<KitchenTicketItemResponse> itemResponses = ticket.getItems().stream()
                .map(this::toTicketItemResponse)
                .toList();

        return new KitchenTicketResponse(
                ticket.getId(),
                ticket.getCafeId(),
                ticket.getOrderId(),
                ticket.getOrderNumber(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                itemResponses
        );
    }

    public KitchenTicketItemResponse toTicketItemResponse(KitchenTicketItem item) {
        Objects.requireNonNull(item, "item must not be null");
        return new KitchenTicketItemResponse(
                item.getId(),
                item.getTicketId(),
                item.getOrderLineId(),
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getStatus(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    public DispatchOrderToKitchenResponse toDispatchResponse(KitchenTicket ticket) {
        Objects.requireNonNull(ticket, "ticket must not be null");
        List<KitchenTicketItemResponse> itemResponses = ticket.getItems().stream()
                .map(this::toTicketItemResponse)
                .toList();

        return new DispatchOrderToKitchenResponse(
                ticket.getId(),
                ticket.getCafeId(),
                ticket.getOrderId(),
                ticket.getOrderNumber(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                itemResponses
        );
    }
}
