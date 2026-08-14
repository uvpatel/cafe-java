package com.cafepos.kitchen.domain;

import com.cafepos.ordering.domain.Order;
import com.cafepos.ordering.domain.OrderType;
import com.cafepos.shared.events.OutboxEvent;
import com.cafepos.shared.events.OutboxEventRepository;
import com.cafepos.shared.events.OutboxEventStatus;
import com.cafepos.shared.money.Money;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KitchenDispatchServiceTest {

    @Mock
    private KitchenTicketRepository kitchenTicketRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private ObjectMapper objectMapper;
    private KitchenDispatchService service;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();
    private final UUID p1 = UUID.randomUUID();
    private final UUID p2 = UUID.randomUUID();
    private final UUID p3 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new KitchenDispatchService(kitchenTicketRepository, outboxEventRepository, objectMapper);
    }

    @Test
    @DisplayName("Should dispatch order creating a new KitchenTicket and outbox event")
    void firstDispatch_Success() throws Exception {
        Order order = new Order(cafeId, sessionId, tableId, "ORD-101", OrderType.DINE_IN);
        order.addOrUpdateLine(p1, "Espresso", Money.of(3.50), 2, true);
        order.addOrUpdateLine(p2, "Napkin", Money.of(0.00), 5, false);

        when(kitchenTicketRepository.findByOrderId(order.getId())).thenReturn(Optional.empty());
        when(kitchenTicketRepository.save(any(KitchenTicket.class))).thenAnswer(inv -> inv.getArgument(0));

        KitchenTicket ticket = service.dispatchOrder(order);

        assertThat(ticket).isNotNull();
        assertThat(ticket.getOrderId()).isEqualTo(order.getId());
        assertThat(ticket.getCafeId()).isEqualTo(cafeId);
        assertThat(ticket.getOrderNumber()).isEqualTo("ORD-101");
        assertThat(ticket.getItems()).hasSize(1);

        KitchenTicketItem item = ticket.getItems().get(0);
        assertThat(item.getProductId()).isEqualTo(p1);
        assertThat(item.getProductName()).isEqualTo("Espresso");
        assertThat(item.getQuantity()).isEqualTo(2);

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());

        OutboxEvent outboxEvent = outboxCaptor.getValue();
        assertThat(outboxEvent.getAggregateType()).isEqualTo("KitchenTicket");
        assertThat(outboxEvent.getAggregateId()).isEqualTo(ticket.getId().toString());
        assertThat(outboxEvent.getEventType()).isEqualTo("KitchenTicketDispatchedEvent");
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(outboxEvent.getPayload()).contains("Espresso");
    }

    @Test
    @DisplayName("Should be idempotent when dispatching an order with no new lines")
    void repeatDispatch_NoNewLines_Idempotent() {
        Order order = new Order(cafeId, sessionId, tableId, "ORD-101", OrderType.DINE_IN);
        order.addOrUpdateLine(p1, "Espresso", Money.of(3.50), 2, true);

        KitchenTicket existingTicket = new KitchenTicket(cafeId, order.getId(), "ORD-101");
        UUID lineId = order.getLines().get(0).getId();
        existingTicket.addItem(lineId, p1, "Espresso", 2);

        when(kitchenTicketRepository.findByOrderId(order.getId())).thenReturn(Optional.of(existingTicket));

        KitchenTicket ticket = service.dispatchOrder(order);

        assertThat(ticket).isSameAs(existingTicket);
        assertThat(ticket.getItems()).hasSize(1);
        verify(kitchenTicketRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should append only unsent order lines when re-dispatching order")
    void repeatDispatch_AppendsOnlyUnsentLines() {
        Order order = new Order(cafeId, sessionId, tableId, "ORD-101", OrderType.DINE_IN);
        order.addOrUpdateLine(p1, "Espresso", Money.of(3.50), 2, true);

        KitchenTicket existingTicket = new KitchenTicket(cafeId, order.getId(), "ORD-101");
        UUID firstLineId = order.getLines().get(0).getId();
        existingTicket.addItem(firstLineId, p1, "Espresso", 2);

        // Now add a second kitchen-routed line to order
        order.addOrUpdateLine(p3, "Cheesecake", Money.of(6.00), 1, true);

        when(kitchenTicketRepository.findByOrderId(order.getId())).thenReturn(Optional.of(existingTicket));
        when(kitchenTicketRepository.save(any(KitchenTicket.class))).thenAnswer(inv -> inv.getArgument(0));

        KitchenTicket ticket = service.dispatchOrder(order);

        assertThat(ticket.getItems()).hasSize(2);
        assertThat(ticket.getItems().stream().map(KitchenTicketItem::getProductName)).containsExactlyInAnyOrder("Espresso", "Cheesecake");

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getPayload()).contains("Cheesecake").doesNotContain("Espresso");
    }

    @Test
    @DisplayName("Should throw exception when order has no kitchen-routed items")
    void dispatchOrder_NoKitchenRoutedItems_ThrowsException() {
        Order order = new Order(cafeId, sessionId, tableId, "ORD-102", OrderType.DINE_IN);
        order.addOrUpdateLine(p2, "Paper Bag", Money.of(0.50), 1, false);

        assertThatThrownBy(() -> service.dispatchOrder(order))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no kitchen-routed items");
    }
}
