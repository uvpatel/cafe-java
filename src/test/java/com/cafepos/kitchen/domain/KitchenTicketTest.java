package com.cafepos.kitchen.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KitchenTicketTest {

    private final UUID cafeId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final String orderNumber = "ORD-1001";

    @Nested
    @DisplayName("Creation and Validation Tests")
    class CreationAndValidationTests {

        @Test
        @DisplayName("Should create KitchenTicket with default TO_COOK status")
        void createKitchenTicket_Success() {
            KitchenTicket ticket = new KitchenTicket(cafeId, orderId, orderNumber);

            assertThat(ticket.getId()).isNotNull();
            assertThat(ticket.getCafeId()).isEqualTo(cafeId);
            assertThat(ticket.getOrderId()).isEqualTo(orderId);
            assertThat(ticket.getOrderNumber()).isEqualTo(orderNumber);
            assertThat(ticket.getStatus()).isEqualTo(KitchenTicketStatus.TO_COOK);
            assertThat(ticket.getItems()).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when null cafeId is provided")
        void nullCafeId_ThrowsException() {
            assertThatThrownBy(() -> new KitchenTicket(null, orderId, orderNumber))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cafe ID must not be null");
        }

        @Test
        @DisplayName("Should throw exception when null orderId is provided")
        void nullOrderId_ThrowsException() {
            assertThatThrownBy(() -> new KitchenTicket(cafeId, null, orderNumber))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Order ID must not be null");
        }

        @Test
        @DisplayName("Should throw exception when blank orderNumber is provided")
        void blankOrderNumber_ThrowsException() {
            assertThatThrownBy(() -> new KitchenTicket(cafeId, orderId, "  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Order number must not be empty");
        }
    }

    @Nested
    @DisplayName("Item Management and Status Progression Tests")
    class ItemManagementAndStatusProgressionTests {

        @Test
        @DisplayName("Should add items and maintain TO_COOK status when all items are TO_COOK")
        void addItem_MaintainsToCookStatus() {
            KitchenTicket ticket = new KitchenTicket(cafeId, orderId, orderNumber);
            UUID lineId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            KitchenTicketItem item = ticket.addItem(lineId, productId, "Espresso", 2);

            assertThat(item).isNotNull();
            assertThat(item.getOrderLineId()).isEqualTo(lineId);
            assertThat(item.getProductId()).isEqualTo(productId);
            assertThat(item.getProductName()).isEqualTo("Espresso");
            assertThat(item.getQuantity()).isEqualTo(2);
            assertThat(item.getStatus()).isEqualTo(KitchenItemStatus.TO_COOK);
            assertThat(ticket.getItems()).hasSize(1);
            assertThat(ticket.getStatus()).isEqualTo(KitchenTicketStatus.TO_COOK);
        }

        @Test
        @DisplayName("Should not duplicate item if added with existing orderLineId")
        void addItem_DuplicateOrderLineId_DoesNotDuplicate() {
            KitchenTicket ticket = new KitchenTicket(cafeId, orderId, orderNumber);
            UUID lineId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            ticket.addItem(lineId, productId, "Espresso", 2);
            ticket.addItem(lineId, productId, "Espresso", 2);

            assertThat(ticket.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("Should transition ticket status to PREPARING when any item starts PREPARING")
        void itemPreparing_TransitionsTicketToPreparing() {
            KitchenTicket ticket = new KitchenTicket(cafeId, orderId, orderNumber);
            UUID line1 = UUID.randomUUID();
            UUID line2 = UUID.randomUUID();

            KitchenTicketItem item1 = ticket.addItem(line1, UUID.randomUUID(), "Espresso", 1);
            ticket.addItem(line2, UUID.randomUUID(), "Latte", 1);

            ticket.updateItemStatus(item1.getId(), KitchenItemStatus.PREPARING);

            assertThat(item1.getStatus()).isEqualTo(KitchenItemStatus.PREPARING);
            assertThat(ticket.getStatus()).isEqualTo(KitchenTicketStatus.PREPARING);
        }

        @Test
        @DisplayName("Should automatically transition ticket status to COMPLETED when all items are COMPLETED")
        void allItemsCompleted_TransitionsTicketToCompleted() {
            KitchenTicket ticket = new KitchenTicket(cafeId, orderId, orderNumber);
            UUID line1 = UUID.randomUUID();
            UUID line2 = UUID.randomUUID();

            KitchenTicketItem item1 = ticket.addItem(line1, UUID.randomUUID(), "Espresso", 1);
            KitchenTicketItem item2 = ticket.addItem(line2, UUID.randomUUID(), "Latte", 1);

            ticket.updateItemStatus(item1.getId(), KitchenItemStatus.COMPLETED);
            assertThat(ticket.getStatus()).isEqualTo(KitchenTicketStatus.PREPARING);

            ticket.updateItemStatus(item2.getId(), KitchenItemStatus.COMPLETED);
            assertThat(ticket.getStatus()).isEqualTo(KitchenTicketStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent item status")
        void updateNonExistentItem_ThrowsException() {
            KitchenTicket ticket = new KitchenTicket(cafeId, orderId, orderNumber);

            assertThatThrownBy(() -> ticket.updateItemStatus(UUID.randomUUID(), KitchenItemStatus.COMPLETED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }
    }
}
