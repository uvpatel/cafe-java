package com.cafepos.receipt.domain;

import com.cafepos.ordering.domain.Order;
import com.cafepos.ordering.domain.OrderTaxLine;
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
@Table(name = "receipts")
public class Receipt {

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "receipt_number", nullable = false, length = 50)
    private String receiptNumber;

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

    @Column(name = "tax_breakdown_json", columnDefinition = "TEXT")
    private String taxBreakdownJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Receipt() {
        // JPA standard constructor
    }

    public Receipt(UUID id, UUID cafeId, UUID orderId, String receiptNumber, Money subtotal, Money discount, Money tax, Money serviceCharge, Money total, String taxBreakdownJson) {
        if (cafeId == null) throw new IllegalArgumentException("cafeId must not be null");
        if (orderId == null) throw new IllegalArgumentException("orderId must not be null");
        if (receiptNumber == null || receiptNumber.isBlank()) throw new IllegalArgumentException("receiptNumber must not be empty");
        if (subtotal == null) throw new IllegalArgumentException("subtotal must not be null");
        if (total == null) throw new IllegalArgumentException("total must not be null");

        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.orderId = orderId;
        this.receiptNumber = receiptNumber.trim();
        this.subtotalAmount = subtotal.amount();
        this.discountAmount = discount != null ? discount.amount() : BigDecimal.ZERO;
        this.taxAmount = tax != null ? tax.amount() : BigDecimal.ZERO;
        this.serviceChargeAmount = serviceCharge != null ? serviceCharge.amount() : BigDecimal.ZERO;
        this.totalAmount = total.amount();
        this.currency = subtotal.currency().getCurrencyCode();
        this.taxBreakdownJson = taxBreakdownJson;
    }

    public Receipt(UUID cafeId, UUID orderId, String receiptNumber, Order order, String taxBreakdownJson) {
        this(UUID.randomUUID(), cafeId, orderId, receiptNumber, order.getSubtotal(), order.getDiscount(), order.getTax(), order.getServiceCharge(), order.getTotal(), taxBreakdownJson);
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

    public UUID getId() {
        return id;
    }

    public UUID getCafeId() {
        return cafeId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getServiceChargeAmount() {
        return serviceChargeAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getTaxBreakdownJson() {
        return taxBreakdownJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Receipt receipt = (Receipt) o;
        return Objects.equals(id, receipt.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
