package com.cafepos.payment.domain;

import com.cafepos.ordering.domain.Order;
import com.cafepos.ordering.domain.OrderRepository;
import com.cafepos.shared.money.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class PaymentDomainService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;

    public PaymentDomainService(PaymentRepository paymentRepository,
                                RefundRepository refundRepository,
                                OrderRepository orderRepository) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "PaymentRepository must not be null");
        this.refundRepository = Objects.requireNonNull(refundRepository, "RefundRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "OrderRepository must not be null");
    }

    @Transactional
    public Payment processPayment(Order order, PaymentMethod paymentMethod, Money paymentAmount, Money tenderedAmount, String transactionReference) {
        Objects.requireNonNull(order, "Order must not be null");
        Objects.requireNonNull(paymentMethod, "PaymentMethod must not be null");
        Objects.requireNonNull(paymentAmount, "PaymentAmount must not be null");

        if (paymentRepository.findByOrderId(order.getId()).isPresent()) {
            throw new IllegalStateException("Payment already exists for order ID: " + order.getId());
        }

        Money orderTotal = order.getTotal();
        if (paymentAmount.amount().compareTo(orderTotal.amount()) != 0) {
            throw new IllegalArgumentException("Payment amount (" + paymentAmount.amount() + ") does not match order total amount (" + orderTotal.amount() + ") exactly");
        }
        if (!paymentAmount.currency().equals(orderTotal.currency())) {
            throw new IllegalArgumentException("Payment amount currency (" + paymentAmount.currency() + ") does not match order total currency (" + orderTotal.currency() + ")");
        }

        Payment payment = new Payment(
                order.getCafeId(),
                order.getId(),
                paymentMethod,
                paymentAmount,
                tenderedAmount,
                transactionReference
        );

        order.markPaid();
        orderRepository.save(order);
        return paymentRepository.save(payment);
    }

    @Transactional
    public Refund processRefund(Payment payment, Money refundAmount, String reason, UUID employeeId) {
        Objects.requireNonNull(payment, "Payment must not be null");
        Objects.requireNonNull(refundAmount, "RefundAmount must not be null");
        Objects.requireNonNull(reason, "Refund reason must not be null");
        Objects.requireNonNull(employeeId, "Employee ID must not be null");

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new IllegalStateException("Payment has already been fully refunded");
        }

        Money paymentAmount = payment.getAmount();
        if (!refundAmount.currency().equals(paymentAmount.currency())) {
            throw new IllegalArgumentException("Refund amount currency (" + refundAmount.currency() + ") does not match payment currency (" + paymentAmount.currency() + ")");
        }

        Money existingRefundsTotal = refundRepository.findByPaymentId(payment.getId()).stream()
                .map(r -> r.getAmount(paymentAmount.currency()))
                .reduce(Money.zero(paymentAmount.currency()), Money::add);

        Money newTotalRefunded = existingRefundsTotal.add(refundAmount);
        if (newTotalRefunded.isGreaterThan(paymentAmount)) {
            throw new IllegalArgumentException("Total requested refund (" + newTotalRefunded.amount() + ") exceeds payment amount (" + paymentAmount.amount() + ")");
        }

        Refund refund = new Refund(
                payment.getCafeId(),
                payment.getId(),
                refundAmount,
                reason,
                employeeId
        );

        if (newTotalRefunded.amount().compareTo(paymentAmount.amount()) == 0) {
            payment.markRefunded();
        }
        paymentRepository.save(payment);
        return refundRepository.save(refund);
    }
}
