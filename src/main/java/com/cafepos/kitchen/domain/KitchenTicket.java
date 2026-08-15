package com.cafepos.kitchen.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "kitchen_tickets")
public class KitchenTicket {

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private KitchenTicketStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private List<KitchenTicketItem> items = new ArrayList<>();

    protected KitchenTicket() {
        // JPA standard constructor
    }

    public KitchenTicket(UUID id, UUID cafeId, UUID orderId, String orderNumber, KitchenTicketStatus status) {
        validateInputs(cafeId, orderId, orderNumber);
        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.orderId = orderId;
        this.orderNumber = orderNumber.trim();
        this.status = status != null ? status : KitchenTicketStatus.TO_COOK;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public KitchenTicket(UUID cafeId, UUID orderId, String orderNumber) {
        this(UUID.randomUUID(), cafeId, orderId, orderNumber, KitchenTicketStatus.TO_COOK);
    }

    private static void validateInputs(UUID cafeId, UUID orderId, String orderNumber) {
        if (cafeId == null) {
            throw new IllegalArgumentException("Cafe ID must not be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID must not be null");
        }
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new IllegalArgumentException("Order number must not be empty");
        }
        if (orderNumber.trim().length() > 50) {
            throw new IllegalArgumentException("Order number must not exceed 50 characters");
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
            this.status = KitchenTicketStatus.TO_COOK;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public KitchenTicketItem addItem(UUID orderLineId, UUID productId, String productName, int quantity) {
        Optional<KitchenTicketItem> existingOpt = items.stream()
                .filter(i -> i.getOrderLineId().equals(orderLineId))
                .findFirst();

        if (existingOpt.isPresent()) {
            return existingOpt.get();
        }

        KitchenTicketItem item = new KitchenTicketItem(this.id, orderLineId, productId, productName, quantity);
        this.items.add(item);
        recalculateStatus();
        return item;
    }

    public void updateItemStatus(UUID itemId, KitchenItemStatus newStatus) {
        KitchenTicketItem item = items.stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Kitchen ticket item with id " + itemId + " not found"));

        item.updateStatus(newStatus);
        recalculateStatus();
    }

    public void recalculateStatus() {
        if (items.isEmpty()) {
            return;
        }

        boolean allCompleted = items.stream().allMatch(i -> i.getStatus() == KitchenItemStatus.COMPLETED);
        if (allCompleted) {
            this.status = KitchenTicketStatus.COMPLETED;
        } else {
            boolean anyInProgress = items.stream().anyMatch(i -> i.getStatus() == KitchenItemStatus.PREPARING || i.getStatus() == KitchenItemStatus.COMPLETED);
            if (anyInProgress) {
                this.status = KitchenTicketStatus.PREPARING;
            } else {
                this.status = KitchenTicketStatus.TO_COOK;
            }
        }
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getCafeId() {
        return cafeId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public KitchenTicketStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isSlaBreached(long targetMinutes, Instant currentTime) {
        if (targetMinutes <= 0) {
            throw new IllegalArgumentException("targetMinutes must be greater than zero");
        }
        Instant referenceTime = currentTime != null ? currentTime : Instant.now();
        Instant deadline = (createdAt != null ? createdAt : referenceTime).plusSeconds(targetMinutes * 60);
        return referenceTime.isAfter(deadline) && status != KitchenTicketStatus.COMPLETED;
    }

    public List<KitchenTicketItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KitchenTicket that = (KitchenTicket) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
