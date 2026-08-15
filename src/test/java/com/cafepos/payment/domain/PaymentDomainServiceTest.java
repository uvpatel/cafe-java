package com.cafepos.payment.domain;

import com.cafepos.ordering.domain.Order;
import com.cafepos.ordering.domain.OrderRepository;
import com.cafepos.ordering.domain.OrderStatus;
import com.cafepos.ordering.domain.OrderType;
import com.cafepos.shared.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentDomainServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private OrderRepository orderRepository;

    private PaymentDomainService paymentDomainService;

    private UUID cafeId;
    private UUID posSessionId;
    private UUID diningTableId;
    private Order order;

    @BeforeEach
    void setUp() {
        paymentDomainService = new PaymentDomainService(paymentRepository, refundRepository, orderRepository);
        cafeId = UUID.randomUUID();
        posSessionId = UUID.randomUUID();
        diningTableId = UUID.randomUUID();

        order = new Order(cafeId, posSessionId, diningTableId, "ORD-101", OrderType.DINE_IN);
        order.addOrUpdateLine(UUID.randomUUID(), "Espresso", Money.of(5.00), 2, true);
    }

    @Test
    @DisplayName("Should return existing payment when duplicate idempotencyKey is presented")
    void shouldReturnExistingPaymentOnDuplicateIdempotencyKey() {
        String idempotencyKey = "IDEM-KEY-999";
        Payment existingPayment = new Payment(cafeId, order.getId(), PaymentMethod.CARD, order.getTotal(), null, "TXN-888", idempotencyKey);
        
        when(paymentRepository.findByCafeIdAndIdempotencyKey(cafeId, idempotencyKey)).thenReturn(Optional.of(existingPayment));

        Payment payment = paymentDomainService.processPayment(order, PaymentMethod.CARD, order.getTotal(), null, "TXN-888", idempotencyKey);

        assertThat(payment).isSameAs(existingPayment);
    }

    @Test
    @DisplayName("Should successfully process payment, mark order PAID, and save entities")
    void shouldProcessPaymentSuccessfully() {
        Money paymentAmount = Money.of(10.00);
        Money tenderedAmount = Money.of(15.00);

        when(paymentRepository.findByOrderId(order.getId())).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentDomainService.processPayment(order, PaymentMethod.CASH, paymentAmount, tenderedAmount, "REF-CASH");

        assertThat(payment).isNotNull();
        assertThat(payment.getAmount()).isEqualTo(paymentAmount);
        assertThat(payment.getTenderedAmount()).isEqualTo(tenderedAmount);
        assertThat(payment.getChangeAmount()).isEqualTo(Money.of(5.00));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

        verify(orderRepository).save(order);
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("Should enforce paid order immutability policy (order lines cannot be mutated once PAID)")
    void shouldEnforcePaidOrderImmutability() {
        Money paymentAmount = Money.of(10.00);

        when(paymentRepository.findByOrderId(order.getId())).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentDomainService.processPayment(order, PaymentMethod.CARD, paymentAmount, null, "TXN-CARD");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

        // Attempting line operations on PAID order must fail
        assertThatThrownBy(() -> order.addOrUpdateLine(UUID.randomUUID(), "Latte", Money.of(4.00), 1, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Order cannot be modified in status: PAID");

        assertThatThrownBy(() -> order.updateLineQuantity(order.getLines().get(0).getProductId(), 3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Order cannot be modified in status: PAID");

        assertThatThrownBy(() -> order.removeLine(order.getLines().get(0).getProductId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Order cannot be modified in status: PAID");
    }

    @Test
    @DisplayName("Should throw exception if payment amount does not match order total exactly")
    void shouldThrowExceptionWhenPaymentAmountMismatchesTotal() {
        Money mismatchedAmount = Money.of(12.00);

        when(paymentRepository.findByOrderId(order.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentDomainService.processPayment(order, PaymentMethod.CARD, mismatchedAmount, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match order total amount");
    }

    @Test
    @DisplayName("Should throw exception if order has already been paid")
    void shouldThrowExceptionWhenPaymentAlreadyExists() {
        Payment existingPayment = new Payment(cafeId, order.getId(), PaymentMethod.CARD, order.getTotal(), null, "REF-1");
        when(paymentRepository.findByOrderId(order.getId())).thenReturn(Optional.of(existingPayment));

        assertThatThrownBy(() -> paymentDomainService.processPayment(order, PaymentMethod.CARD, order.getTotal(), null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Payment already exists for order ID");
    }

    @Test
    @DisplayName("Should process refund, update payment status to REFUNDED, and create immutable Refund record")
    void shouldProcessRefundSuccessfully() {
        Money paymentAmount = Money.of(10.00);
        Payment payment = new Payment(cafeId, order.getId(), PaymentMethod.CARD, paymentAmount, null, "TXN-123");
        UUID employeeId = UUID.randomUUID();

        when(refundRepository.findByPaymentId(payment.getId())).thenReturn(java.util.List.of());
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Refund refund = paymentDomainService.processRefund(payment, paymentAmount, "Wrong item served", employeeId);

        assertThat(refund).isNotNull();
        assertThat(refund.getPaymentId()).isEqualTo(payment.getId());
        assertThat(refund.getAmount()).isEqualTo(BigDecimal.valueOf(10.00).setScale(4));
        assertThat(refund.getReason()).isEqualTo("Wrong item served");
        assertThat(refund.getRefundedByEmployeeId()).isEqualTo(employeeId);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentRepository).save(payment);
        verify(refundRepository).save(refund);
    }

    @Test
    @DisplayName("Should throw exception when processing refund for already fully refunded payment")
    void shouldThrowExceptionWhenRefundingAlreadyRefundedPayment() {
        Money paymentAmount = Money.of(10.00);
        Payment payment = new Payment(cafeId, order.getId(), PaymentMethod.CARD, paymentAmount, null, "TXN-123");
        payment.markRefunded();

        assertThatThrownBy(() -> paymentDomainService.processRefund(payment, paymentAmount, "Reason", UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Payment has already been fully refunded");
    }

    @Test
    @DisplayName("Should allow partial refund and throw exception when cumulative refund exceeds payment total")
    void shouldValidatePartialRefundLimits() {
        Money paymentAmount = Money.of(10.00);
        Payment payment = new Payment(cafeId, order.getId(), PaymentMethod.CARD, paymentAmount, null, "TXN-123");
        UUID employeeId = UUID.randomUUID();

        Refund existingRefund = new Refund(cafeId, payment.getId(), Money.of(7.00), "First partial refund", employeeId);
        when(refundRepository.findByPaymentId(payment.getId())).thenReturn(java.util.List.of(existingRefund));

        assertThatThrownBy(() -> paymentDomainService.processRefund(payment, Money.of(5.00), "Second refund attempt", employeeId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds payment amount");
    }
}
