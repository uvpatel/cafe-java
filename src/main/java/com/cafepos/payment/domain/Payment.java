package com.cafepos.payment.domain;

import com.cafepos.shared.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "tendered_amount", precision = 19, scale = 4)
    private BigDecimal tenderedAmount;

    @Column(name = "change_amount", precision = 19, scale = 4)
    private BigDecimal changeAmount;

    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Payment() {
        // JPA standard constructor
    }

    public Payment(UUID id, UUID cafeId, UUID orderId, PaymentMethod paymentMethod, Money amount, Money tenderedAmount, Money changeAmount, String transactionReference) {
        validateInputs(cafeId, orderId, paymentMethod, amount);

        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.orderId = orderId;
        this.paymentMethod = paymentMethod;
        this.amount = amount.amount();
        this.currency = amount.currency().getCurrencyCode();

        if (paymentMethod == PaymentMethod.CASH) {
            if (tenderedAmount == null) {
                throw new IllegalArgumentException("Tendered amount is required for CASH payment");
            }
            if (!tenderedAmount.currency().getCurrencyCode().equalsIgnoreCase(this.currency)) {
                throw new IllegalArgumentException("Tendered amount currency mismatch");
            }
            if (tenderedAmount.amount().compareTo(this.amount) < 0) {
                throw new IllegalArgumentException("Tendered amount cannot be less than order total amount");
            }
            this.tenderedAmount = tenderedAmount.amount();

            Money calculatedChange = tenderedAmount.subtract(amount);
            this.changeAmount = calculatedChange.amount();
        } else {
            this.tenderedAmount = null;
            this.changeAmount = null;
        }

        this.transactionReference = (transactionReference != null && !transactionReference.isBlank()) ? transactionReference.trim() : null;
        this.status = PaymentStatus.COMPLETED;
    }

    public Payment(UUID id, UUID cafeId, UUID orderId, PaymentMethod paymentMethod, Money amount, Money tenderedAmount, Money changeAmount, String transactionReference, String idempotencyKey) {
        this(id, cafeId, orderId, paymentMethod, amount, tenderedAmount, changeAmount, transactionReference);
        this.idempotencyKey = (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey.trim() : null;
    }

    public Payment(UUID cafeId, UUID orderId, PaymentMethod paymentMethod, Money amount, Money tenderedAmount, String transactionReference, String idempotencyKey) {
        this(UUID.randomUUID(), cafeId, orderId, paymentMethod, amount, tenderedAmount, null, transactionReference, idempotencyKey);
    }

    public Payment(UUID cafeId, UUID orderId, PaymentMethod paymentMethod, Money amount, Money tenderedAmount, String transactionReference) {
        this(UUID.randomUUID(), cafeId, orderId, paymentMethod, amount, tenderedAmount, null, transactionReference, null);
    }

    private static void validateInputs(UUID cafeId, UUID orderId, PaymentMethod paymentMethod, Money amount) {
        if (cafeId == null) {
            throw new IllegalArgumentException("Cafe ID must not be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID must not be null");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method must not be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }
        if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
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

    public void markRefunded() {
        if (this.status == PaymentStatus.REFUNDED) {
            throw new IllegalStateException("Payment is already refunded");
        }
        this.status = PaymentStatus.REFUNDED;
    }

    public Currency getCurrency() {
        return Currency.getInstance(this.currency);
    }

    public Money getAmount() {
        return new Money(this.amount, getCurrency());
    }

    public Money getTenderedAmount() {
        return tenderedAmount != null ? new Money(this.tenderedAmount, getCurrency()) : null;
    }

    public Money getChangeAmount() {
        return changeAmount != null ? new Money(this.changeAmount, getCurrency()) : null;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UUID getCafeId() {
        return cafeId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return Objects.equals(id, payment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
