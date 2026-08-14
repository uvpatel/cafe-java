package com.cafepos.ordering.domain;

import com.cafepos.shared.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private final UUID cafeId = UUID.randomUUID();
    private final UUID posSessionId = UUID.randomUUID();
    private final UUID diningTableId = UUID.randomUUID();
    private final UUID productId1 = UUID.randomUUID();
    private final UUID productId2 = UUID.randomUUID();

    @Test
    @DisplayName("Should create draft DINE_IN order successfully")
    void createDraftOrderSuccess() {
        Order order = new Order(cafeId, posSessionId, diningTableId, "ORD-001", OrderType.DINE_IN);

        assertThat(order.getId()).isNotNull();
        assertThat(order.getCafeId()).isEqualTo(cafeId);
        assertThat(order.getPosSessionId()).isEqualTo(posSessionId);
        assertThat(order.getDiningTableId()).isEqualTo(diningTableId);
        assertThat(order.getOrderNumber()).isEqualTo("ORD-001");
        assertThat(order.getOrderType()).isEqualTo(OrderType.DINE_IN);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);
        assertThat(order.getSubtotal().amount()).isEqualByComparingTo("0.0000");
        assertThat(order.getTotal().amount()).isEqualByComparingTo("0.0000");
        assertThat(order.getLines()).isEmpty();
        assertThat(order.getTaxLines()).isEmpty();
    }

    @Test
    @DisplayName("Should throw exception when creating DINE_IN order without table ID")
    void dineInOrderRequiresTableId() {
        assertThatThrownBy(() -> new Order(cafeId, posSessionId, null, "ORD-002", OrderType.DINE_IN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Dining table ID is required for DINE_IN orders");
    }

    @Test
    @DisplayName("Should allow TAKEAWAY order without dining table ID")
    void takeawayOrderSuccess() {
        Order order = new Order(cafeId, posSessionId, null, "ORD-003", OrderType.TAKEAWAY);
        assertThat(order.getOrderType()).isEqualTo(OrderType.TAKEAWAY);
        assertThat(order.getDiningTableId()).isNull();
    }

    @Test
    @DisplayName("Should add, update, and remove order lines and recalculate subtotals deterministically")
    void addUpdateRemoveLinesAndRecalculate() {
        Order order = new Order(cafeId, posSessionId, diningTableId, "ORD-004", OrderType.DINE_IN);

        // Add first product (2 x $4.50 = $9.00)
        order.addOrUpdateLine(productId1, "Americano", Money.of(4.50), 2, true);
        assertThat(order.getLines()).hasSize(1);
        assertThat(order.getSubtotal().amount()).isEqualByComparingTo("9.0000");
        assertThat(order.getTotal().amount()).isEqualByComparingTo("9.0000");

        // Add second product (1 x $3.00 = $3.00)
        order.addOrUpdateLine(productId2, "Croissant", Money.of(3.00), 1, false);
        assertThat(order.getLines()).hasSize(2);
        assertThat(order.getSubtotal().amount()).isEqualByComparingTo("12.0000");

        // Add same product again (adds 1 to quantity -> 3 x $4.50 = $13.50 + $3.00 = $16.50)
        order.addOrUpdateLine(productId1, "Americano", Money.of(4.50), 1, true);
        assertThat(order.getLines()).hasSize(2);
        assertThat(order.getLines().stream().filter(l -> l.getProductId().equals(productId1)).findFirst().get().getQuantity()).isEqualTo(3);
        assertThat(order.getSubtotal().amount()).isEqualByComparingTo("16.5000");

        // Explicitly set quantity of product 2 to 4 (4 x $3.00 = $12.00 + $13.50 = $25.50)
        order.updateLineQuantity(productId2, 4);
        assertThat(order.getSubtotal().amount()).isEqualByComparingTo("25.5000");

        // Remove product 1 ($25.50 - $13.50 = $12.00)
        order.removeLine(productId1);
        assertThat(order.getLines()).hasSize(1);
        assertThat(order.getSubtotal().amount()).isEqualByComparingTo("12.0000");
    }

    @Test
    @DisplayName("Should apply discounts and taxes correctly to totals")
    void applyDiscountAndTaxes() {
        Order order = new Order(cafeId, posSessionId, diningTableId, "ORD-005", OrderType.DINE_IN);
        order.addOrUpdateLine(productId1, "Latte", Money.of(10.00), 2, true); // Subtotal = $20.00

        // Set discount $2.00
        order.setDiscount(Money.of(2.00));
        assertThat(order.getDiscount().amount()).isEqualByComparingTo("2.0000");
        assertThat(order.getTotal().amount()).isEqualByComparingTo("18.0000");

        // Add tax line (10% Sales Tax = $2.00)
        OrderTaxLine salesTax = new OrderTaxLine(order.getId(), "Sales Tax 10%", new BigDecimal("10.0000"), new BigDecimal("2.0000"));
        order.setTaxLines(List.of(salesTax));

        assertThat(order.getTax().amount()).isEqualByComparingTo("2.0000");
        // Total = $20.00 (subtotal) - $2.00 (discount) + $2.00 (tax) = $20.00
        assertThat(order.getTotal().amount()).isEqualByComparingTo("20.0000");
    }

    @Test
    @DisplayName("Should handle order status transition lifecycle correctly")
    void statusTransitions() {
        Order order = new Order(cafeId, posSessionId, diningTableId, "ORD-006", OrderType.DINE_IN);
        order.addOrUpdateLine(productId1, "Tea", Money.of(2.50), 1, true);

        // DRAFT -> CONFIRMED
        order.confirm();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        // CONFIRMED -> SENT_TO_KITCHEN
        order.markSentToKitchen();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SENT_TO_KITCHEN);

        // Cannot modify order sent to kitchen
        assertThatThrownBy(() -> order.addOrUpdateLine(productId2, "Muffin", Money.of(3.00), 1, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Order cannot be modified in status: SENT_TO_KITCHEN");

        // SENT_TO_KITCHEN -> READY
        order.markReady();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY);

        // READY -> PAID
        order.markPaid();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

        // Cannot cancel paid order
        assertThatThrownBy(() -> order.cancel())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel an order that is already PAID");
    }

    @Test
    @DisplayName("Should throw exception when confirming an empty order")
    void confirmEmptyOrderFails() {
        Order order = new Order(cafeId, posSessionId, diningTableId, "ORD-007", OrderType.DINE_IN);

        assertThatThrownBy(order::confirm)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot confirm an empty order");
    }
}
