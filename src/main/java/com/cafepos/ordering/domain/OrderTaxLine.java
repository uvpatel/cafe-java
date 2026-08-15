package com.cafepos.ordering.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "order_tax_lines")
public class OrderTaxLine {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "tax_name_snapshot", nullable = false, length = 100)
    private String taxNameSnapshot;

    @Column(name = "rate_percent", nullable = false, precision = 7, scale = 4)
    private BigDecimal ratePercent;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxAmount;

    protected OrderTaxLine() {
        // JPA standard constructor
    }

    public OrderTaxLine(UUID id, UUID orderId, String taxNameSnapshot, BigDecimal ratePercent, BigDecimal taxAmount) {
        if (taxNameSnapshot == null || taxNameSnapshot.isBlank()) {
            throw new IllegalArgumentException("Tax name snapshot must not be empty");
        }
        if (ratePercent == null || ratePercent.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Rate percent must not be null or negative");
        }
        if (taxAmount == null || taxAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Tax amount must not be null or negative");
        }

        this.id = id != null ? id : UUID.randomUUID();
        this.orderId = orderId;
        this.taxNameSnapshot = taxNameSnapshot.trim();
        this.ratePercent = ratePercent;
        this.taxAmount = taxAmount;
    }

    public OrderTaxLine(UUID orderId, String taxNameSnapshot, BigDecimal ratePercent, BigDecimal taxAmount) {
        this(UUID.randomUUID(), orderId, taxNameSnapshot, ratePercent, taxAmount);
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

    public String getTaxNameSnapshot() {
        return taxNameSnapshot;
    }

    public BigDecimal getRatePercent() {
        return ratePercent;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public com.cafepos.shared.money.Money getTaxAmountMoney(java.util.Currency currency) {
        return new com.cafepos.shared.money.Money(this.taxAmount, currency);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderTaxLine that = (OrderTaxLine) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
