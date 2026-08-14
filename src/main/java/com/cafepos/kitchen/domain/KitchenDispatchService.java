package com.cafepos.kitchen.domain;

import com.cafepos.ordering.domain.Order;
import com.cafepos.ordering.domain.OrderLine;

import com.cafepos.shared.events.OutboxEvent;
import com.cafepos.shared.events.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class KitchenDispatchService {

    private final KitchenTicketRepository kitchenTicketRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public KitchenDispatchService(KitchenTicketRepository kitchenTicketRepository,
                                  OutboxEventRepository outboxEventRepository,
                                  ObjectMapper objectMapper) {
        this.kitchenTicketRepository = Objects.requireNonNull(kitchenTicketRepository, "kitchenTicketRepository must not be null");
        this.outboxEventRepository = Objects.requireNonNull(outboxEventRepository, "outboxEventRepository must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null").copy().findAndRegisterModules();
    }

    public KitchenTicket dispatchOrder(Order order) {
        Objects.requireNonNull(order, "order must not be null");

        List<OrderLine> kitchenLines = order.getLines().stream()
                .filter(OrderLine::isKitchenRouted)
                .toList();

        if (kitchenLines.isEmpty()) {
            throw new IllegalArgumentException("Order contains no kitchen-routed items to dispatch");
        }

        Optional<KitchenTicket> existingTicketOpt = kitchenTicketRepository.findByOrderId(order.getId());
        KitchenTicket ticket;
        List<KitchenTicketDispatchedEvent.ItemDispatched> newlyDispatchedItems = new ArrayList<>();

        if (existingTicketOpt.isEmpty()) {
            ticket = new KitchenTicket(order.getCafeId(), order.getId(), order.getOrderNumber());
            for (OrderLine line : kitchenLines) {
                KitchenTicketItem item = ticket.addItem(line.getId(), line.getProductId(), line.getProductNameSnapshot(), line.getQuantity());
                newlyDispatchedItems.add(new KitchenTicketDispatchedEvent.ItemDispatched(
                        item.getId(), line.getId(), line.getProductId(), line.getProductNameSnapshot(), line.getQuantity()
                ));
            }
        } else {
            ticket = existingTicketOpt.get();
            Set<UUID> dispatchedOrderLineIds = ticket.getItems().stream()
                    .map(KitchenTicketItem::getOrderLineId)
                    .collect(Collectors.toSet());

            for (OrderLine line : kitchenLines) {
                if (!dispatchedOrderLineIds.contains(line.getId())) {
                    KitchenTicketItem item = ticket.addItem(line.getId(), line.getProductId(), line.getProductNameSnapshot(), line.getQuantity());
                    newlyDispatchedItems.add(new KitchenTicketDispatchedEvent.ItemDispatched(
                            item.getId(), line.getId(), line.getProductId(), line.getProductNameSnapshot(), line.getQuantity()
                    ));
                }
            }
        }

        if (newlyDispatchedItems.isEmpty()) {
            return ticket;
        }

        KitchenTicket savedTicket = kitchenTicketRepository.save(ticket);

        KitchenTicketDispatchedEvent dispatchedEvent = new KitchenTicketDispatchedEvent(
                savedTicket.getId(),
                savedTicket.getCafeId(),
                savedTicket.getOrderId(),
                savedTicket.getOrderNumber(),
                newlyDispatchedItems,
                Instant.now()
        );

        try {
            String payload = objectMapper.writeValueAsString(dispatchedEvent);
            OutboxEvent outboxEvent = new OutboxEvent(
                    "KitchenTicket",
                    savedTicket.getId().toString(),
                    "KitchenTicketDispatchedEvent",
                    payload
            );
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Failed to serialize KitchenTicketDispatchedEvent payload", e);
        }

        return savedTicket;
    }
}
