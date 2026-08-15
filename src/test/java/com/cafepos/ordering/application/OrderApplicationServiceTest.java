package com.cafepos.ordering.application;

import com.cafepos.catalog.domain.Product;
import com.cafepos.catalog.domain.ProductRepository;
import com.cafepos.catalog.domain.UnitOfMeasure;
import com.cafepos.floor.domain.DiningTable;
import com.cafepos.floor.domain.DiningTableRepository;
import com.cafepos.ordering.api.AddOrderLineRequest;
import com.cafepos.ordering.api.AssignTableRequest;
import com.cafepos.ordering.api.CreateOrderRequest;
import com.cafepos.ordering.api.OrderDtoMapper;
import com.cafepos.ordering.api.OrderResponse;
import com.cafepos.ordering.domain.Order;
import com.cafepos.ordering.domain.OrderNumberGenerator;
import com.cafepos.ordering.domain.OrderRepository;
import com.cafepos.ordering.domain.OrderStatus;
import com.cafepos.ordering.domain.OrderType;
import com.cafepos.session.domain.PosSession;
import com.cafepos.session.domain.PosSessionRepository;
import com.cafepos.shared.error.BusinessException;
import com.cafepos.shared.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PosSessionRepository posSessionRepository;

    @Mock
    private DiningTableRepository diningTableRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderNumberGenerator orderNumberGenerator;

    private OrderDtoMapper dtoMapper;
    private OrderApplicationService service;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();
    private final UUID floorId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        dtoMapper = new OrderDtoMapper();
        service = new OrderApplicationService(
                orderRepository,
                posSessionRepository,
                diningTableRepository,
                productRepository,
                orderNumberGenerator,
                dtoMapper
        );
    }

    @Test
    @DisplayName("Should create draft DINE_IN order successfully when session open and table available")
    void createDineInOrderSuccess() {
        PosSession session = new PosSession(sessionId, cafeId, UUID.randomUUID(), employeeId, BigDecimal.TEN);
        DiningTable table = new DiningTable(tableId, cafeId, floorId, "T-1", 4, true);

        when(posSessionRepository.findByIdAndCafeId(sessionId, cafeId)).thenReturn(Optional.of(session));
        when(diningTableRepository.findByIdAndCafeId(tableId, cafeId)).thenReturn(Optional.of(table));
        when(orderRepository.findByCafeIdAndDiningTableIdAndStatusIn(eq(cafeId), eq(tableId), any()))
                .thenReturn(Optional.empty());
        when(orderNumberGenerator.generateOrderNumber(cafeId)).thenReturn("ORD-20260814-0001");
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateOrderRequest request = new CreateOrderRequest(sessionId, OrderType.DINE_IN, tableId, "Near window", "USD");
        OrderResponse response = service.createOrder(cafeId, request);

        assertThat(response).isNotNull();
        assertThat(response.orderNumber()).isEqualTo("ORD-20260814-0001");
        assertThat(response.orderType()).isEqualTo(OrderType.DINE_IN);
        assertThat(response.status()).isEqualTo(OrderStatus.DRAFT);
        assertThat(response.diningTableId()).isEqualTo(tableId);
        assertThat(response.notes()).isEqualTo("Near window");
    }

    @Test
    @DisplayName("Should throw BusinessException TABLE_OCCUPIED when table has an active order")
    void createOrderFailsWhenTableOccupied() {
        PosSession session = new PosSession(sessionId, cafeId, UUID.randomUUID(), employeeId, BigDecimal.TEN);
        DiningTable table = new DiningTable(tableId, cafeId, floorId, "T-1", 4, true);
        Order existingOrder = new Order(cafeId, sessionId, tableId, "ORD-20260814-0001", OrderType.DINE_IN);

        when(posSessionRepository.findByIdAndCafeId(sessionId, cafeId)).thenReturn(Optional.of(session));
        when(diningTableRepository.findByIdAndCafeId(tableId, cafeId)).thenReturn(Optional.of(table));
        when(orderRepository.findByCafeIdAndDiningTableIdAndStatusIn(eq(cafeId), eq(tableId), any()))
                .thenReturn(Optional.of(existingOrder));

        CreateOrderRequest request = new CreateOrderRequest(sessionId, OrderType.DINE_IN, tableId, null, null);

        assertThatThrownBy(() -> service.createOrder(cafeId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Table is currently occupied")
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo("TABLE_OCCUPIED");
    }

    @Test
    @DisplayName("Should catch DataIntegrityViolationException and throw BusinessException TABLE_OCCUPIED")
    void createOrderCatchesConstraintViolation() {
        PosSession session = new PosSession(sessionId, cafeId, UUID.randomUUID(), employeeId, BigDecimal.TEN);
        DiningTable table = new DiningTable(tableId, cafeId, floorId, "T-1", 4, true);

        when(posSessionRepository.findByIdAndCafeId(sessionId, cafeId)).thenReturn(Optional.of(session));
        when(diningTableRepository.findByIdAndCafeId(tableId, cafeId)).thenReturn(Optional.of(table));
        when(orderRepository.findByCafeIdAndDiningTableIdAndStatusIn(eq(cafeId), eq(tableId), any()))
                .thenReturn(Optional.empty());
        when(orderNumberGenerator.generateOrderNumber(cafeId)).thenReturn("ORD-20260814-0001");
        when(orderRepository.saveAndFlush(any(Order.class))).thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

        CreateOrderRequest request = new CreateOrderRequest(sessionId, OrderType.DINE_IN, tableId, null, null);

        assertThatThrownBy(() -> service.createOrder(cafeId, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo("TABLE_OCCUPIED");
    }

    @Test
    @DisplayName("Should add order line by fetching product snapshots")
    void addOrderLineSuccess() {
        Order order = new Order(cafeId, sessionId, tableId, "ORD-20260814-0001", OrderType.DINE_IN);
        Product product = new Product(productId, cafeId, categoryId, "Espresso", "ESP-001", Money.of(3.50), UnitOfMeasure.UNIT, true);

        when(orderRepository.findByIdAndCafeId(order.getId(), cafeId)).thenReturn(Optional.of(order));
        when(productRepository.findByIdAndCafeId(productId, cafeId)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        AddOrderLineRequest request = new AddOrderLineRequest(productId, 2);
        OrderResponse response = service.addOrderLine(cafeId, order.getId(), request);

        assertThat(response.lines()).hasSize(1);
        assertThat(response.lines().get(0).productNameSnapshot()).isEqualTo("Espresso");
        assertThat(response.lines().get(0).unitPriceSnapshot()).isEqualByComparingTo("3.5000");
        assertThat(response.lines().get(0).quantity()).isEqualTo(2);
        assertThat(response.lines().get(0).isKitchenRouted()).isTrue();
        assertThat(response.subtotalAmount()).isEqualByComparingTo("7.0000");
    }

    @Test
    @DisplayName("Should throw INVALID_ORDER_STATE when adding line to order sent to kitchen")
    void addOrderLineFailsForUnmodifiableOrder() {
        Order order = new Order(cafeId, sessionId, tableId, "ORD-20260814-0001", OrderType.DINE_IN);
        order.addOrUpdateLine(productId, "Espresso", Money.of(3.50), 1, true);
        order.confirm();
        order.markSentToKitchen();

        when(orderRepository.findByIdAndCafeId(order.getId(), cafeId)).thenReturn(Optional.of(order));

        AddOrderLineRequest request = new AddOrderLineRequest(productId, 2);

        assertThatThrownBy(() -> service.addOrderLine(cafeId, order.getId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo("INVALID_ORDER_STATE");
    }

    @Test
    @DisplayName("Should assign table ensuring concurrency occupancy protection")
    void assignTableSuccess() {
        Order order = new Order(cafeId, sessionId, null, "ORD-20260814-0002", OrderType.TAKEAWAY);
        DiningTable table = new DiningTable(tableId, cafeId, floorId, "T-2", 2, true);

        when(orderRepository.findByIdAndCafeId(order.getId(), cafeId)).thenReturn(Optional.of(order));
        when(diningTableRepository.findByIdAndCafeId(tableId, cafeId)).thenReturn(Optional.of(table));
        when(orderRepository.findByCafeIdAndDiningTableIdAndStatusIn(eq(cafeId), eq(tableId), any()))
                .thenReturn(Optional.empty());
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        AssignTableRequest request = new AssignTableRequest(tableId);
        OrderResponse response = service.assignTable(cafeId, order.getId(), request);

        assertThat(response.diningTableId()).isEqualTo(tableId);
    }

    @Test
    @DisplayName("Should cancel draft order successfully")
    void cancelOrderSuccess() {
        Order order = new Order(cafeId, sessionId, tableId, "ORD-20260814-0001", OrderType.DINE_IN);

        when(orderRepository.findByIdAndCafeId(order.getId(), cafeId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = service.cancelOrder(cafeId, order.getId());

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
    }
}
