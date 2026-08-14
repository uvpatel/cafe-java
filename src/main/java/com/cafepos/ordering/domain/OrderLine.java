package com.cafepos.ordering.domain;

import com.cafepos.shared.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "order_lines")
public class OrderLine {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name_snapshot", nullable = false, length = 100)
    private String productNameSnapshot;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPriceSnapshot;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "line_subtotal", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineSubtotal;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineTotal;

    @Column(name = "is_kitchen_routed", nullable = false)
    private boolean kitchenRouted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OrderLine() {
        // JPA standard constructor
    }

    public OrderLine(UUID id, UUID orderId, UUID productId, String productNameSnapshot, Money unitPriceSnapshot, int quantity, boolean kitchenRouted) {
        validateInputs(productId, productNameSnapshot, unitPriceSnapshot, quantity);

        this.id = id != null ? id : UUID.randomUUID();
        this.orderId = orderId;
        this.productId = productId;
        this.productNameSnapshot = productNameSnapshot.trim();
        this.unitPriceSnapshot = unitPriceSnapshot.amount();
        this.quantity = quantity;
        this.kitchenRouted = kitchenRouted;

        calculateAmounts(unitPriceSnapshot.currency());
    }

    public OrderLine(UUID orderId, UUID productId, String productNameSnapshot, Money unitPriceSnapshot, int quantity, boolean kitchenRouted) {
        this(UUID.randomUUID(), orderId, productId, productNameSnapshot, unitPriceSnapshot, quantity, kitchenRouted);
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    private static void validateInputs(UUID productId, String productNameSnapshot, Money unitPriceSnapshot, int quantity) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID must not be null");
        }
        if (productNameSnapshot == null || productNameSnapshot.isBlank()) {
            throw new IllegalArgumentException("Product name snapshot must not be empty");
        }
        if (productNameSnapshot.trim().length() > 100) {
            throw new IllegalArgumentException("Product name snapshot must not exceed 100 characters");
        }
        if (unitPriceSnapshot == null || unitPriceSnapshot.amount() == null) {
            throw new IllegalArgumentException("Unit price snapshot must not be null");
        }
        if (unitPriceSnapshot.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price snapshot must not be negative");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }

    public void updateQuantity(int newQuantity, Currency currency) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        this.quantity = newQuantity;
        calculateAmounts(currency);
    }

    public void calculateAmounts(Currency currency) {
        Money unitPrice = getUnitPriceSnapshot(currency);
        Money subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.lineSubtotal = subtotal.amount();
        // lineTotal is lineSubtotal for now (no line-level discount)
        this.lineTotal = subtotal.amount();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public BigDecimal getUnitPriceSnapshotAmount() {
        return unitPriceSnapshot;
    }

    public Money getUnitPriceSnapshot(Currency currency) {
        return new Money(this.unitPriceSnapshot, currency);
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineSubtotalAmount() {
        return lineSubtotal;
    }

    public Money getLineSubtotal(Currency currency) {
        return new Money(this.lineSubtotal, currency);
    }

    public BigDecimal getLineTotalAmount() {
        return lineTotal;
    }

    public Money getLineTotal(Currency currency) {
        return new Money(this.lineTotal, currency);
    }

    public boolean isKitchenRouted() {
        return kitchenRouted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderLine orderLine = (OrderLine) o;
        return Objects.equals(id, orderLine.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
