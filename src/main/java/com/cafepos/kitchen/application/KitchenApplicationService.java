package com.cafepos.kitchen.application;

import com.cafepos.kitchen.api.DispatchOrderToKitchenResponse;

import com.cafepos.kitchen.api.KitchenDtoMapper;
import com.cafepos.kitchen.api.KitchenTicketResponse;
import com.cafepos.kitchen.domain.KitchenDispatchService;
import com.cafepos.kitchen.domain.KitchenItemStatus;
import com.cafepos.kitchen.domain.KitchenTicket;
import com.cafepos.kitchen.domain.KitchenTicketItem;
import com.cafepos.kitchen.domain.KitchenTicketItemRepository;
import com.cafepos.kitchen.domain.KitchenTicketRepository;
import com.cafepos.kitchen.domain.KitchenTicketStatus;
import com.cafepos.ordering.domain.Order;
import com.cafepos.ordering.domain.OrderRepository;
import com.cafepos.ordering.domain.OrderStatus;
import com.cafepos.shared.error.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class KitchenApplicationService implements DispatchOrderToKitchenUseCase, ManageKitchenTicketUseCase, GetKitchenTicketsUseCase {

    private final OrderRepository orderRepository;
    private final KitchenTicketRepository kitchenTicketRepository;
    private final KitchenTicketItemRepository kitchenTicketItemRepository;
    private final KitchenDispatchService kitchenDispatchService;
    private final KitchenDtoMapper dtoMapper;

    public KitchenApplicationService(OrderRepository orderRepository,
                                     KitchenTicketRepository kitchenTicketRepository,
                                     KitchenTicketItemRepository kitchenTicketItemRepository,
                                     KitchenDispatchService kitchenDispatchService,
                                     KitchenDtoMapper dtoMapper) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.kitchenTicketRepository = Objects.requireNonNull(kitchenTicketRepository, "kitchenTicketRepository must not be null");
        this.kitchenTicketItemRepository = Objects.requireNonNull(kitchenTicketItemRepository, "kitchenTicketItemRepository must not be null");
        this.kitchenDispatchService = Objects.requireNonNull(kitchenDispatchService, "kitchenDispatchService must not be null");
        this.dtoMapper = Objects.requireNonNull(dtoMapper, "dtoMapper must not be null");
    }

    @Override
    public DispatchOrderToKitchenResponse dispatchOrderToKitchen(UUID cafeId, UUID orderId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");

        Order order = orderRepository.findByIdAndCafeId(orderId, cafeId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found with id " + orderId, 404));

        if (order.getStatus() == OrderStatus.DRAFT) {
            order.confirm();
            order.markSentToKitchen();
            orderRepository.save(order);
        } else if (order.getStatus() == OrderStatus.CONFIRMED) {
            order.markSentToKitchen();
            orderRepository.save(order);
        }

        try {
            KitchenTicket ticket = kitchenDispatchService.dispatchOrder(order);
            return dtoMapper.toDispatchResponse(ticket);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("NO_KITCHEN_ITEMS", ex.getMessage(), 400);
        }
    }

    @Override
    public KitchenTicketResponse startPreparing(UUID cafeId, UUID ticketId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(ticketId, "ticketId must not be null");

        KitchenTicket ticket = kitchenTicketRepository.findByIdAndCafeId(ticketId, cafeId)
                .orElseThrow(() -> new BusinessException("TICKET_NOT_FOUND", "Kitchen ticket not found with id " + ticketId, 404));

        for (KitchenTicketItem item : ticket.getItems()) {
            if (item.getStatus() == KitchenItemStatus.TO_COOK) {
                ticket.updateItemStatus(item.getId(), KitchenItemStatus.PREPARING);
            }
        }
        ticket.recalculateStatus();
        KitchenTicket saved = kitchenTicketRepository.save(ticket);
        return dtoMapper.toTicketResponse(saved);
    }

    @Override
    public KitchenTicketResponse completeTicket(UUID cafeId, UUID ticketId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(ticketId, "ticketId must not be null");

        KitchenTicket ticket = kitchenTicketRepository.findByIdAndCafeId(ticketId, cafeId)
                .orElseThrow(() -> new BusinessException("TICKET_NOT_FOUND", "Kitchen ticket not found with id " + ticketId, 404));

        for (KitchenTicketItem item : ticket.getItems()) {
            if (item.getStatus() != KitchenItemStatus.COMPLETED) {
                ticket.updateItemStatus(item.getId(), KitchenItemStatus.COMPLETED);
            }
        }
        ticket.recalculateStatus();
        KitchenTicket saved = kitchenTicketRepository.save(ticket);
        return dtoMapper.toTicketResponse(saved);
    }

    @Override
    public KitchenTicketResponse completeItem(UUID cafeId, UUID itemId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(itemId, "itemId must not be null");

        KitchenTicketItem item = kitchenTicketItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException("ITEM_NOT_FOUND", "Kitchen ticket item not found with id " + itemId, 404));

        KitchenTicket ticket = kitchenTicketRepository.findByIdAndCafeId(item.getTicketId(), cafeId)
                .orElseThrow(() -> new BusinessException("TICKET_NOT_FOUND", "Kitchen ticket not found with id " + item.getTicketId(), 404));

        ticket.updateItemStatus(itemId, KitchenItemStatus.COMPLETED);
        KitchenTicket saved = kitchenTicketRepository.save(ticket);
        return dtoMapper.toTicketResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KitchenTicketResponse> getKitchenTickets(UUID cafeId, KitchenTicketStatus status) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");

        List<KitchenTicket> tickets;
        if (status != null) {
            tickets = kitchenTicketRepository.findByCafeIdAndStatus(cafeId, status);
        } else {
            tickets = kitchenTicketRepository.findByCafeId(cafeId);
        }

        return tickets.stream()
                .map(dtoMapper::toTicketResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public KitchenTicketResponse getKitchenTicketById(UUID cafeId, UUID ticketId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(ticketId, "ticketId must not be null");

        KitchenTicket ticket = kitchenTicketRepository.findByIdAndCafeId(ticketId, cafeId)
                .orElseThrow(() -> new BusinessException("TICKET_NOT_FOUND", "Kitchen ticket not found with id " + ticketId, 404));

        return dtoMapper.toTicketResponse(ticket);
    }
}
