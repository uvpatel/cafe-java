package com.cafepos.ordering.api;

import com.cafepos.ordering.domain.Order;
import com.cafepos.ordering.domain.OrderLine;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class OrderDtoMapper {

    public OrderResponse toOrderResponse(Order order) {
        Objects.requireNonNull(order, "order must not be null");
        List<OrderLineResponse> lineResponses = order.getLines().stream()
                .map(this::toOrderLineResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCafeId(),
                order.getPosSessionId(),
                order.getDiningTableId(),
                order.getOrderNumber(),
                order.getOrderType(),
                order.getStatus(),
                order.getSubtotal().amount(),
                order.getDiscount().amount(),
                order.getTax().amount(),
                order.getTotal().amount(),
                order.getCurrency().getCurrencyCode(),
                order.getNotes(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                lineResponses
        );
    }

    public OrderLineResponse toOrderLineResponse(OrderLine line) {
        Objects.requireNonNull(line, "line must not be null");
        return new OrderLineResponse(
                line.getId(),
                line.getOrderId(),
                line.getProductId(),
                line.getProductNameSnapshot(),
                line.getUnitPriceSnapshotAmount(),
                line.getQuantity(),
                line.getLineSubtotalAmount(),
                line.getLineTotalAmount(),
                line.isKitchenRouted(),
                line.getCreatedAt()
        );
    }
}
