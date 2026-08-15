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
import com.cafepos.ordering.domain.OrderType;
import com.cafepos.shared.error.BusinessException;
import com.cafepos.shared.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KitchenApplicationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private KitchenTicketRepository kitchenTicketRepository;

    @Mock
    private KitchenTicketItemRepository kitchenTicketItemRepository;

    @Mock
    private KitchenDispatchService kitchenDispatchService;

    private KitchenDtoMapper dtoMapper;
    private KitchenApplicationService kitchenApplicationService;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID posSessionId = UUID.randomUUID();
    private final UUID diningTableId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        dtoMapper = new KitchenDtoMapper();
        kitchenApplicationService = new KitchenApplicationService(
                orderRepository,
                kitchenTicketRepository,
                kitchenTicketItemRepository,
                kitchenDispatchService,
                dtoMapper
        );
    }

    @Test
    @DisplayName("dispatchOrderToKitchen should confirm draft order, set SENT_TO_KITCHEN, and return dispatch response")
    void shouldDispatchOrderToKitchen() {
        Order order = new Order(orderId, cafeId, posSessionId, diningTableId, "ORD-001", OrderType.DINE_IN, "USD");
        order.addOrUpdateLine(productId, "Latte", Money.of(new BigDecimal("4.50")), 2, true);

        KitchenTicket ticket = new KitchenTicket(cafeId, orderId, "ORD-001");
        KitchenTicketItem item = ticket.addItem(order.getLines().get(0).getId(), productId, "Latte", 2);
        assertThat(item.getQuantity()).isEqualTo(2);

        when(orderRepository.findByIdAndCafeId(orderId, cafeId)).thenReturn(Optional.of(order));
        when(kitchenDispatchService.dispatchOrder(order)).thenReturn(ticket);

        DispatchOrderToKitchenResponse response = kitchenApplicationService.dispatchOrderToKitchen(cafeId, orderId);

        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.orderNumber()).isEqualTo("ORD-001");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productName()).isEqualTo("Latte");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SENT_TO_KITCHEN);

        verify(orderRepository).save(order);
        verify(kitchenDispatchService).dispatchOrder(order);
    }

    @Test
    @DisplayName("dispatchOrderToKitchen with non-existent order should throw BusinessException(404)")
    void shouldThrowWhenDispatchOrderNotFound() {
        when(orderRepository.findByIdAndCafeId(orderId, cafeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> kitchenApplicationService.dispatchOrderToKitchen(cafeId, orderId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    @DisplayName("startPreparing should set all TO_COOK items to PREPARING and update ticket status")
    void shouldStartPreparingTicket() {
        KitchenTicket ticket = new KitchenTicket(cafeId, orderId, "ORD-001");
        ticket.addItem(UUID.randomUUID(), productId, "Burger", 1);

        when(kitchenTicketRepository.findByIdAndCafeId(ticket.getId(), cafeId)).thenReturn(Optional.of(ticket));
        when(kitchenTicketRepository.save(any(KitchenTicket.class))).thenAnswer(i -> i.getArgument(0));

        KitchenTicketResponse response = kitchenApplicationService.startPreparing(cafeId, ticket.getId());

        assertThat(response.status()).isEqualTo(KitchenTicketStatus.PREPARING);
        assertThat(response.items().get(0).status()).isEqualTo(KitchenItemStatus.PREPARING);
    }

    @Test
    @DisplayName("completeTicket should set all items to COMPLETED and update ticket status to COMPLETED")
    void shouldCompleteTicket() {
        KitchenTicket ticket = new KitchenTicket(cafeId, orderId, "ORD-001");
        ticket.addItem(UUID.randomUUID(), productId, "Burger", 1);

        when(kitchenTicketRepository.findByIdAndCafeId(ticket.getId(), cafeId)).thenReturn(Optional.of(ticket));
        when(kitchenTicketRepository.save(any(KitchenTicket.class))).thenAnswer(i -> i.getArgument(0));

        KitchenTicketResponse response = kitchenApplicationService.completeTicket(cafeId, ticket.getId());

        assertThat(response.status()).isEqualTo(KitchenTicketStatus.COMPLETED);
        assertThat(response.items().get(0).status()).isEqualTo(KitchenItemStatus.COMPLETED);
    }

    @Test
    @DisplayName("completeItem should mark single item COMPLETED")
    void shouldCompleteSingleItem() {
        KitchenTicket ticket = new KitchenTicket(cafeId, orderId, "ORD-001");
        KitchenTicketItem item = ticket.addItem(UUID.randomUUID(), productId, "Fries", 1);

        when(kitchenTicketItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(kitchenTicketRepository.findByIdAndCafeId(ticket.getId(), cafeId)).thenReturn(Optional.of(ticket));
        when(kitchenTicketRepository.save(any(KitchenTicket.class))).thenAnswer(i -> i.getArgument(0));

        KitchenTicketResponse response = kitchenApplicationService.completeItem(cafeId, item.getId());

        assertThat(response.status()).isEqualTo(KitchenTicketStatus.COMPLETED);
        assertThat(response.items().get(0).status()).isEqualTo(KitchenItemStatus.COMPLETED);
    }

    @Test
    @DisplayName("getKitchenTickets should return list of active tickets for cafe")
    void shouldGetKitchenTickets() {
        KitchenTicket ticket = new KitchenTicket(cafeId, orderId, "ORD-001");
        when(kitchenTicketRepository.findByCafeIdAndStatus(cafeId, KitchenTicketStatus.TO_COOK))
                .thenReturn(List.of(ticket));

        List<KitchenTicketResponse> tickets = kitchenApplicationService.getKitchenTickets(cafeId, KitchenTicketStatus.TO_COOK);

        assertThat(tickets).hasSize(1);
        assertThat(tickets.get(0).orderNumber()).isEqualTo("ORD-001");
    }
}
