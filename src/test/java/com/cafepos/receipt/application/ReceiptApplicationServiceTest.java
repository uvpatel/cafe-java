package com.cafepos.receipt.application;

import com.cafepos.ordering.domain.Order;
import com.cafepos.ordering.domain.OrderRepository;
import com.cafepos.ordering.domain.OrderStatus;
import com.cafepos.ordering.domain.OrderType;
import com.cafepos.payment.domain.Payment;
import com.cafepos.payment.domain.PaymentMethod;
import com.cafepos.payment.domain.PaymentRepository;
import com.cafepos.receipt.api.ReceiptDtoMapper;
import com.cafepos.receipt.api.ReceiptResponse;
import com.cafepos.shared.error.BusinessException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiptApplicationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    private final ReceiptDtoMapper receiptDtoMapper = new ReceiptDtoMapper();

    private ReceiptApplicationService receiptApplicationService;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        receiptApplicationService = new ReceiptApplicationService(
                orderRepository,
                paymentRepository,
                receiptDtoMapper
        );
    }

    @Test
    @DisplayName("Should generate receipt response for paid order")
    void shouldGenerateReceiptForPaidOrder() {
        Order order = new Order(orderId, cafeId, sessionId, null, "ORD-1001", OrderType.TAKEAWAY, "USD");
        order.addOrUpdateLine(productId, "Latte", new Money(new BigDecimal("5.00"), order.getCurrency()), 2, false);
        order.confirm();
        order.markPaid();

        Money totalMoney = order.getTotal();
        Payment payment = new Payment(paymentId, cafeId, orderId, PaymentMethod.CASH, totalMoney, totalMoney, new Money(BigDecimal.ZERO, order.getCurrency()), null);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        ReceiptResponse receipt = receiptApplicationService.getReceiptForOrder(cafeId, orderId);

        assertThat(receipt).isNotNull();
        assertThat(receipt.orderId()).isEqualTo(orderId);
        assertThat(receipt.orderNumber()).isEqualTo("ORD-1001");
        assertThat(receipt.orderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(receipt.payment().paymentId()).isEqualTo(paymentId);
        assertThat(receipt.lines()).hasSize(1);
        assertThat(receipt.lines().get(0).productName()).isEqualTo("Latte");
    }

    @Test
    @DisplayName("Should throw BusinessException when order is not paid")
    void shouldThrowExceptionWhenOrderNotPaid() {
        Order order = new Order(orderId, cafeId, sessionId, null, "ORD-1001", OrderType.TAKEAWAY, "USD");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> receiptApplicationService.getReceiptForOrder(cafeId, orderId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Receipt can only be generated for paid orders");
    }

    @Test
    @DisplayName("Should throw BusinessException when order is not found")
    void shouldThrowExceptionWhenOrderNotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> receiptApplicationService.getReceiptForOrder(cafeId, orderId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Order not found with id " + orderId);
    }
}
