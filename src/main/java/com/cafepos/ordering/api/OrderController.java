package com.cafepos.ordering.api;

import com.cafepos.ordering.application.AssignTableUseCase;
import com.cafepos.ordering.application.CancelOrderUseCase;
import com.cafepos.ordering.application.CreateOrderUseCase;
import com.cafepos.ordering.application.GetOrderUseCase;
import com.cafepos.ordering.application.ManageOrderLinesUseCase;
import com.cafepos.ordering.domain.OrderStatus;
import com.cafepos.shared.tenancy.TenantContextHolder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final ManageOrderLinesUseCase manageOrderLinesUseCase;
    private final AssignTableUseCase assignTableUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase,
                           ManageOrderLinesUseCase manageOrderLinesUseCase,
                           AssignTableUseCase assignTableUseCase,
                           CancelOrderUseCase cancelOrderUseCase,
                           GetOrderUseCase getOrderUseCase) {
        this.createOrderUseCase = Objects.requireNonNull(createOrderUseCase, "createOrderUseCase must not be null");
        this.manageOrderLinesUseCase = Objects.requireNonNull(manageOrderLinesUseCase, "manageOrderLinesUseCase must not be null");
        this.assignTableUseCase = Objects.requireNonNull(assignTableUseCase, "assignTableUseCase must not be null");
        this.cancelOrderUseCase = Objects.requireNonNull(cancelOrderUseCase, "cancelOrderUseCase must not be null");
        this.getOrderUseCase = Objects.requireNonNull(getOrderUseCase, "getOrderUseCase must not be null");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        OrderResponse response = createOrderUseCase.createOrder(cafeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN', 'MANAGER', 'KITCHEN_STAFF')")
    public ResponseEntity<List<OrderResponse>> getOrders(
            @RequestParam(name = "posSessionId", required = false) UUID posSessionId,
            @RequestParam(name = "status", required = false) OrderStatus status) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        List<OrderResponse> responses = getOrderUseCase.getOrders(cafeId, posSessionId, status);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN', 'MANAGER', 'KITCHEN_STAFF')")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable("id") UUID id) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        OrderResponse response = getOrderUseCase.getOrderById(cafeId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> addOrderLine(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AddOrderLineRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        OrderResponse response = manageOrderLinesUseCase.addOrderLine(cafeId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/lines/{lineId}")
    @PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> updateOrderLine(
            @PathVariable("id") UUID id,
            @PathVariable("lineId") UUID lineId,
            @Valid @RequestBody UpdateOrderLineRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        OrderResponse response = manageOrderLinesUseCase.updateOrderLine(cafeId, id, lineId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> deleteOrderLine(
            @PathVariable("id") UUID id,
            @PathVariable("lineId") UUID lineId) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        OrderResponse response = manageOrderLinesUseCase.deleteOrderLine(cafeId, id, lineId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/table")
    @PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> assignTable(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AssignTableRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        OrderResponse response = assignTableUseCase.assignTable(cafeId, id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable("id") UUID id) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        OrderResponse response = cancelOrderUseCase.cancelOrder(cafeId, id);
        return ResponseEntity.ok(response);
    }
}
