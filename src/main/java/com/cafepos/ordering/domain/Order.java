package com.cafepos.ordering.domain;

import com.cafepos.shared.money.Money;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "pos_session_id", nullable = false)
    private UUID posSessionId;

    @Column(name = "dining_table_id")
    private UUID diningTableId;

    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountAmount;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxAmount;

    @Column(name = "service_charge_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal serviceChargeAmount;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "notes", length = 255)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private List<OrderLine> lines = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private List<OrderTaxLine> taxLines = new ArrayList<>();

    protected Order() {
        // JPA standard constructor
    }

    public Order(UUID id, UUID cafeId, UUID posSessionId, UUID diningTableId, String orderNumber, OrderType orderType, String currency) {
        validateInitialInputs(cafeId, posSessionId, orderNumber, orderType, currency);
        if (orderType == OrderType.DINE_IN && diningTableId == null) {
            throw new IllegalArgumentException("Dining table ID is required for DINE_IN orders");
        }

        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.posSessionId = posSessionId;
        this.diningTableId = diningTableId;
        this.orderNumber = orderNumber.trim();
        this.orderType = orderType;
        this.status = OrderStatus.DRAFT;
        this.currency = currency != null ? currency.toUpperCase().trim() : Money.DEFAULT_CURRENCY.getCurrencyCode();

        Currency curr = getCurrency();
        this.subtotalAmount = Money.zero(curr).amount();
        this.discountAmount = Money.zero(curr).amount();
        this.taxAmount = Money.zero(curr).amount();
        this.serviceChargeAmount = Money.zero(curr).amount();
        this.totalAmount = Money.zero(curr).amount();
    }

    public Order(UUID cafeId, UUID posSessionId, UUID diningTableId, String orderNumber, OrderType orderType) {
        this(UUID.randomUUID(), cafeId, posSessionId, diningTableId, orderNumber, orderType, Money.DEFAULT_CURRENCY.getCurrencyCode());
    }

    private static void validateInitialInputs(UUID cafeId, UUID posSessionId, String orderNumber, OrderType orderType, String currency) {
        if (cafeId == null) {
            throw new IllegalArgumentException("Cafe ID must not be null");
        }
        if (posSessionId == null) {
            throw new IllegalArgumentException("POS Session ID must not be null");
        }
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new IllegalArgumentException("Order number must not be empty");
        }
        if (orderType == null) {
            throw new IllegalArgumentException("Order type must not be null");
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
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Currency getCurrency() {
        return Currency.getInstance(this.currency);
    }

    public Money getSubtotal() {
        return new Money(this.subtotalAmount, getCurrency());
    }

    public Money getDiscount() {
        return new Money(this.discountAmount, getCurrency());
    }

    public Money getTax() {
        return new Money(this.taxAmount, getCurrency());
    }

    public Money getServiceCharge() {
        if (this.serviceChargeAmount == null) {
            return Money.zero(getCurrency());
        }
        return new Money(this.serviceChargeAmount, getCurrency());
    }

    public Money getTotal() {
        return new Money(this.totalAmount, getCurrency());
    }

    public void addOrUpdateLine(UUID productId, String productName, Money unitPrice, int quantity, boolean kitchenRouted) {
        assertModifiable();
        if (!unitPrice.currency().getCurrencyCode().equalsIgnoreCase(this.currency)) {
            throw new IllegalArgumentException("Unit price currency mismatch: expected " + this.currency + " but got " + unitPrice.currency().getCurrencyCode());
        }

        Optional<OrderLine> existingLineOpt = lines.stream()
                .filter(l -> l.getProductId().equals(productId))
                .findFirst();

        if (existingLineOpt.isPresent()) {
            OrderLine existingLine = existingLineOpt.get();
            existingLine.updateQuantity(existingLine.getQuantity() + quantity, getCurrency());
        } else {
            OrderLine line = new OrderLine(this.id, productId, productName, unitPrice, quantity, kitchenRouted);
            lines.add(line);
        }
        recalculateTotals();
    }

    public void updateLineQuantity(UUID productId, int quantity) {
        assertModifiable();
        OrderLine line = lines.stream()
                .filter(l -> l.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Order line with product ID " + productId + " not found"));

        if (quantity <= 0) {
            lines.remove(line);
        } else {
            line.updateQuantity(quantity, getCurrency());
        }
        recalculateTotals();
    }

    public void removeLine(UUID productId) {
        assertModifiable();
        boolean removed = lines.removeIf(l -> l.getProductId().equals(productId));
        if (!removed) {
            throw new IllegalArgumentException("Order line with product ID " + productId + " not found");
        }
        recalculateTotals();
    }

    public void setDiscount(Money discount) {
        assertModifiable();
        if (discount == null) {
            throw new IllegalArgumentException("Discount must not be null");
        }
        if (!discount.currency().getCurrencyCode().equalsIgnoreCase(this.currency)) {
            throw new IllegalArgumentException("Discount currency mismatch: expected " + this.currency + " but got " + discount.currency().getCurrencyCode());
        }
        if (discount.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Discount amount cannot be negative");
        }
        this.discountAmount = discount.amount();
        recalculateTotals();
    }

    public void setTaxLines(List<OrderTaxLine> newTaxLines) {
        assertModifiable();
        this.taxLines.clear();
        if (newTaxLines != null) {
            for (OrderTaxLine taxLine : newTaxLines) {
                taxLine.setOrderId(this.id);
                this.taxLines.add(taxLine);
            }
        }
        recalculateTotals();
    }

    public void recalculateTotals() {
        Currency curr = getCurrency();

        // Sum line subtotals
        Money calculatedSubtotal = Money.zero(curr);
        for (OrderLine line : lines) {
            line.calculateAmounts(curr);
            calculatedSubtotal = calculatedSubtotal.add(line.getLineTotal(curr));
        }
        this.subtotalAmount = calculatedSubtotal.amount();

        // Calculate tax sum
        BigDecimal calculatedTaxAmount = BigDecimal.ZERO;
        for (OrderTaxLine taxLine : taxLines) {
            calculatedTaxAmount = calculatedTaxAmount.add(taxLine.getTaxAmount());
        }
        Money taxMoney = new Money(calculatedTaxAmount, curr);
        this.taxAmount = taxMoney.amount();

        if (this.serviceChargeAmount == null) {
            this.serviceChargeAmount = Money.zero(curr).amount();
        }

        // Total = Subtotal - Discount + Tax + ServiceCharge
        Money discountMoney = new Money(this.discountAmount, curr);
        Money serviceChargeMoney = new Money(this.serviceChargeAmount, curr);

        Money calculatedTotal = calculatedSubtotal
                .subtract(discountMoney)
                .add(taxMoney)
                .add(serviceChargeMoney);

        if (calculatedTotal.amount().compareTo(BigDecimal.ZERO) < 0) {
            calculatedTotal = Money.zero(curr);
        }

        this.totalAmount = calculatedTotal.amount();
    }

    public void applyServiceChargePercent(BigDecimal percentage) {
        assertModifiable();
        if (percentage == null || percentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Service charge percentage cannot be negative");
        }

        Money subtotal = getSubtotal();
        BigDecimal factor = percentage.divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP);
        this.serviceChargeAmount = subtotal.multiply(factor).amount();
        recalculateTotals();
    }

    public void confirm() {
        if (this.status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Cannot confirm order in status: " + this.status);
        }
        if (this.lines.isEmpty()) {
            throw new IllegalStateException("Cannot confirm an empty order");
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void markSentToKitchen() {
        if (this.status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Order must be CONFIRMED before sending to kitchen");
        }
        this.status = OrderStatus.SENT_TO_KITCHEN;
    }

    public void markReady() {
        if (this.status != OrderStatus.SENT_TO_KITCHEN && this.status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Order must be CONFIRMED or SENT_TO_KITCHEN to be marked READY");
        }
        this.status = OrderStatus.READY;
    }

    public void markPaid() {
        if (this.status == OrderStatus.CANCELLED || this.status == OrderStatus.PAID) {
            throw new IllegalStateException("Cannot mark paid for order in status: " + this.status);
        }
        this.status = OrderStatus.PAID;
    }

    public void cancel() {
        if (this.status == OrderStatus.PAID) {
            throw new IllegalStateException("Cannot cancel an order that is already PAID");
        }
        this.status = OrderStatus.CANCELLED;
    }

    private void assertModifiable() {
        if (this.status != OrderStatus.DRAFT && this.status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Order cannot be modified in status: " + this.status);
        }
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public UUID getCafeId() {
        return cafeId;
    }

    public UUID getPosSessionId() {
        return posSessionId;
    }

    public UUID getDiningTableId() {
        return diningTableId;
    }

    public void setDiningTableId(UUID diningTableId) {
        assertModifiable();
        if (this.orderType == OrderType.DINE_IN && diningTableId == null) {
            throw new IllegalArgumentException("Dining table ID is required for DINE_IN orders");
        }
        this.diningTableId = diningTableId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        assertModifiable();
        this.notes = (notes != null && !notes.isBlank()) ? notes.trim() : null;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public List<OrderTaxLine> getTaxLines() {
        return Collections.unmodifiableList(taxLines);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
