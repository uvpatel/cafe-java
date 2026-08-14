package com.cafepos.ordering.application;

import com.cafepos.catalog.domain.Product;
import com.cafepos.catalog.domain.ProductRepository;
import com.cafepos.floor.domain.DiningTableRepository;
import com.cafepos.ordering.api.AddOrderLineRequest;
import com.cafepos.ordering.api.AssignTableRequest;
import com.cafepos.ordering.api.CreateOrderRequest;
import com.cafepos.ordering.api.OrderDtoMapper;
import com.cafepos.ordering.api.OrderResponse;
import com.cafepos.ordering.api.UpdateOrderLineRequest;
import com.cafepos.ordering.domain.Order;
import com.cafepos.ordering.domain.OrderLine;
import com.cafepos.ordering.domain.OrderNumberGenerator;
import com.cafepos.ordering.domain.OrderRepository;
import com.cafepos.ordering.domain.OrderStatus;
import com.cafepos.ordering.domain.OrderType;
import com.cafepos.session.domain.PosSession;
import com.cafepos.session.domain.PosSessionRepository;
import com.cafepos.shared.error.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class OrderApplicationService implements CreateOrderUseCase, ManageOrderLinesUseCase, AssignTableUseCase, CancelOrderUseCase, GetOrderUseCase {

    private static final List<OrderStatus> ACTIVE_TABLE_STATUSES = List.of(
            OrderStatus.DRAFT,
            OrderStatus.CONFIRMED,
            OrderStatus.SENT_TO_KITCHEN,
            OrderStatus.READY
    );

    private final OrderRepository orderRepository;
    private final PosSessionRepository posSessionRepository;
    private final DiningTableRepository diningTableRepository;
    private final ProductRepository productRepository;
    private final OrderNumberGenerator orderNumberGenerator;
    private final OrderDtoMapper dtoMapper;

    public OrderApplicationService(OrderRepository orderRepository,
                                   PosSessionRepository posSessionRepository,
                                   DiningTableRepository diningTableRepository,
                                   ProductRepository productRepository,
                                   OrderNumberGenerator orderNumberGenerator,
                                   OrderDtoMapper dtoMapper) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.posSessionRepository = Objects.requireNonNull(posSessionRepository, "posSessionRepository must not be null");
        this.diningTableRepository = Objects.requireNonNull(diningTableRepository, "diningTableRepository must not be null");
        this.productRepository = Objects.requireNonNull(productRepository, "productRepository must not be null");
        this.orderNumberGenerator = Objects.requireNonNull(orderNumberGenerator, "orderNumberGenerator must not be null");
        this.dtoMapper = Objects.requireNonNull(dtoMapper, "dtoMapper must not be null");
    }

    @Override
    public OrderResponse createOrder(UUID cafeId, CreateOrderRequest request) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        PosSession session = posSessionRepository.findByIdAndCafeId(request.posSessionId(), cafeId)
                .orElseThrow(() -> new BusinessException("SESSION_NOT_FOUND", "POS Session not found with id " + request.posSessionId(), 404));

        if (!session.isOpen()) {
            throw new BusinessException("SESSION_CLOSED", "POS Session is closed", 400);
        }

        if (request.orderType() == OrderType.DINE_IN) {
            if (request.diningTableId() == null) {
                throw new BusinessException("INVALID_ORDER", "Dining table ID is required for DINE_IN orders", 400);
            }

            diningTableRepository.findByIdAndCafeId(request.diningTableId(), cafeId)
                    .orElseThrow(() -> new BusinessException("TABLE_NOT_FOUND", "Dining table not found with id " + request.diningTableId(), 404));

            Optional<Order> activeOrder = orderRepository.findByCafeIdAndDiningTableIdAndStatusIn(cafeId, request.diningTableId(), ACTIVE_TABLE_STATUSES);
            if (activeOrder.isPresent()) {
                throw new BusinessException("TABLE_OCCUPIED", "Table is currently occupied by active order " + activeOrder.get().getOrderNumber(), 409);
            }
        }

        String orderNumber = orderNumberGenerator.generateOrderNumber(cafeId);
        Order order = new Order(cafeId, request.posSessionId(), request.diningTableId(), orderNumber, request.orderType());
        if (request.notes() != null && !request.notes().isBlank()) {
            order.setNotes(request.notes());
        }

        try {
            Order saved = orderRepository.saveAndFlush(order);
            return dtoMapper.toOrderResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("TABLE_OCCUPIED", "Table is currently occupied by another order", 409);
        }
    }

    @Override
    public OrderResponse addOrderLine(UUID cafeId, UUID orderId, AddOrderLineRequest request) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        Order order = orderRepository.findByIdAndCafeId(orderId, cafeId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found with id " + orderId, 404));

        assertOrderModifiable(order);

        Product product = productRepository.findByIdAndCafeId(request.productId(), cafeId)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Product not found with id " + request.productId(), 404));

        if (product.isArchived()) {
            throw new BusinessException("PRODUCT_ARCHIVED", "Cannot add archived product to order", 400);
        }

        order.addOrUpdateLine(product.getId(), product.getName(), product.getPrice(), request.quantity(), product.isKitchenRouted());
        Order saved = orderRepository.save(order);
        return dtoMapper.toOrderResponse(saved);
    }

    @Override
    public OrderResponse updateOrderLine(UUID cafeId, UUID orderId, UUID lineId, UpdateOrderLineRequest request) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(lineId, "lineId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        Order order = orderRepository.findByIdAndCafeId(orderId, cafeId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found with id " + orderId, 404));

        assertOrderModifiable(order);

        OrderLine line = order.getLines().stream()
                .filter(l -> l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("LINE_NOT_FOUND", "Order line not found with id " + lineId, 404));

        if (request.quantity() <= 0) {
            order.removeLine(line.getProductId());
        } else {
            order.updateLineQuantity(line.getProductId(), request.quantity());
        }

        Order saved = orderRepository.save(order);
        return dtoMapper.toOrderResponse(saved);
    }

    @Override
    public OrderResponse deleteOrderLine(UUID cafeId, UUID orderId, UUID lineId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(lineId, "lineId must not be null");

        Order order = orderRepository.findByIdAndCafeId(orderId, cafeId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found with id " + orderId, 404));

        assertOrderModifiable(order);

        OrderLine line = order.getLines().stream()
                .filter(l -> l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("LINE_NOT_FOUND", "Order line not found with id " + lineId, 404));

        order.removeLine(line.getProductId());
        Order saved = orderRepository.save(order);
        return dtoMapper.toOrderResponse(saved);
    }

    @Override
    public OrderResponse assignTable(UUID cafeId, UUID orderId, AssignTableRequest request) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        Order order = orderRepository.findByIdAndCafeId(orderId, cafeId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found with id " + orderId, 404));

        assertOrderModifiable(order);

        diningTableRepository.findByIdAndCafeId(request.diningTableId(), cafeId)
                .orElseThrow(() -> new BusinessException("TABLE_NOT_FOUND", "Dining table not found with id " + request.diningTableId(), 404));

        Optional<Order> activeOrder = orderRepository.findByCafeIdAndDiningTableIdAndStatusIn(cafeId, request.diningTableId(), ACTIVE_TABLE_STATUSES);
        if (activeOrder.isPresent() && !activeOrder.get().getId().equals(orderId)) {
            throw new BusinessException("TABLE_OCCUPIED", "Table is currently occupied by active order " + activeOrder.get().getOrderNumber(), 409);
        }

        order.setDiningTableId(request.diningTableId());

        try {
            Order saved = orderRepository.saveAndFlush(order);
            return dtoMapper.toOrderResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("TABLE_OCCUPIED", "Table is currently occupied by another order", 409);
        }
    }

    @Override
    public OrderResponse cancelOrder(UUID cafeId, UUID orderId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");

        Order order = orderRepository.findByIdAndCafeId(orderId, cafeId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found with id " + orderId, 404));

        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("INVALID_ORDER_STATE", "Cannot cancel order in status: " + order.getStatus(), 400);
        }

        order.cancel();
        Order saved = orderRepository.save(order);
        return dtoMapper.toOrderResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID cafeId, UUID orderId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");

        Order order = orderRepository.findByIdAndCafeId(orderId, cafeId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found with id " + orderId, 404));

        return dtoMapper.toOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(UUID cafeId, UUID posSessionId, OrderStatus status) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");

        List<Order> orders;
        if (posSessionId != null && status != null) {
            orders = orderRepository.findByCafeIdAndPosSessionId(cafeId, posSessionId).stream()
                    .filter(o -> o.getStatus() == status)
                    .toList();
        } else if (posSessionId != null) {
            orders = orderRepository.findByCafeIdAndPosSessionId(cafeId, posSessionId);
        } else if (status != null) {
            orders = orderRepository.findByCafeIdAndStatus(cafeId, status);
        } else {
            orders = orderRepository.findByCafeId(cafeId);
        }

        return orders.stream()
                .map(dtoMapper::toOrderResponse)
                .toList();
    }

    private void assertOrderModifiable(Order order) {
        if (order.getStatus() != OrderStatus.DRAFT && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException("INVALID_ORDER_STATE", "Order lines can only be modified when order is in DRAFT or CONFIRMED state", 400);
        }
    }
}
