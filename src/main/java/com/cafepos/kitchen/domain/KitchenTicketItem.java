package com.cafepos.kitchen.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "kitchen_ticket_items")
public class KitchenTicketItem {

    @Id
    private UUID id;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "order_line_id", nullable = false)
    private UUID orderLineId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private KitchenItemStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected KitchenTicketItem() {
        // JPA standard constructor
    }

    public KitchenTicketItem(UUID id, UUID ticketId, UUID orderLineId, UUID productId, String productName, int quantity, KitchenItemStatus status) {
        validateInputs(orderLineId, productId, productName, quantity);

        this.id = id != null ? id : UUID.randomUUID();
        this.ticketId = ticketId;
        this.orderLineId = orderLineId;
        this.productId = productId;
        this.productName = productName.trim();
        this.quantity = quantity;
        this.status = status != null ? status : KitchenItemStatus.TO_COOK;
    }

    public KitchenTicketItem(UUID ticketId, UUID orderLineId, UUID productId, String productName, int quantity) {
        this(UUID.randomUUID(), ticketId, orderLineId, productId, productName, quantity, KitchenItemStatus.TO_COOK);
    }

    private static void validateInputs(UUID orderLineId, UUID productId, String productName, int quantity) {
        if (orderLineId == null) {
            throw new IllegalArgumentException("Order line ID must not be null");
        }
        if (productId == null) {
            throw new IllegalArgumentException("Product ID must not be null");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("Product name must not be empty");
        }
        if (productName.trim().length() > 100) {
            throw new IllegalArgumentException("Product name must not exceed 100 characters");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = KitchenItemStatus.TO_COOK;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void updateStatus(KitchenItemStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("New status must not be null");
        }
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    void setTicketId(UUID ticketId) {
        this.ticketId = ticketId;
    }

    public UUID getOrderLineId() {
        return orderLineId;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public KitchenItemStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KitchenTicketItem that = (KitchenTicketItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
