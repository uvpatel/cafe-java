package com.cafepos.payment.application;

import com.cafepos.ordering.domain.Order;
import com.cafepos.ordering.domain.OrderRepository;
import com.cafepos.ordering.domain.OrderType;
import com.cafepos.payment.api.PaymentDtoMapper;
import com.cafepos.payment.api.PaymentResponse;
import com.cafepos.payment.api.ProcessCardPaymentRequest;
import com.cafepos.payment.api.ProcessCashPaymentRequest;
import com.cafepos.payment.api.ProcessUpiConfirmationRequest;
import com.cafepos.payment.api.RefundRequest;
import com.cafepos.payment.api.RefundResponse;
import com.cafepos.payment.api.UpiIntentResponse;
import com.cafepos.payment.domain.Payment;
import com.cafepos.payment.domain.PaymentDomainService;
import com.cafepos.payment.domain.PaymentMethod;
import com.cafepos.payment.domain.PaymentRepository;
import com.cafepos.payment.domain.Refund;
import com.cafepos.payment.infrastructure.UpiQrGeneratorService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentApplicationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentDomainService paymentDomainService;

    @Mock
    private UpiQrGeneratorService upiQrGeneratorService;

    private final PaymentDtoMapper paymentDtoMapper = new PaymentDtoMapper();

    private PaymentApplicationService paymentApplicationService;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        paymentApplicationService = new PaymentApplicationService(
                orderRepository,
                paymentRepository,
                paymentDomainService,
                upiQrGeneratorService,
                paymentDtoMapper
        );

        sampleOrder = new Order(orderId, cafeId, sessionId, null, "ORD-1001", OrderType.TAKEAWAY, "USD");
        sampleOrder.addOrUpdateLine(productId, "Coffee", new Money(new BigDecimal("10.00"), sampleOrder.getCurrency()), 2, false);
    }

    @Test
    @DisplayName("Should process cash payment with change calculation successfully")
    void shouldProcessCashPaymentWithChangeCalculation() {
        ProcessCashPaymentRequest request = new ProcessCashPaymentRequest(new BigDecimal("25.00"));
        Money orderTotal = sampleOrder.getTotal(); // 20.00
        Money tenderedAmount = new Money(new BigDecimal("25.00"), sampleOrder.getCurrency());

        Payment payment = new Payment(paymentId, cafeId, orderId, PaymentMethod.CASH, orderTotal, tenderedAmount, new Money(new BigDecimal("5.00"), sampleOrder.getCurrency()), null);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        when(paymentDomainService.processPayment(eq(sampleOrder), eq(PaymentMethod.CASH), eq(orderTotal), eq(tenderedAmount), eq(null)))
                .thenReturn(payment);

        PaymentResponse response = paymentApplicationService.processCashPayment(cafeId, orderId, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(paymentId);
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(response.tenderedAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(response.changeAmount()).isEqualByComparingTo(new BigDecimal("5.00"));

        verify(paymentDomainService).processPayment(eq(sampleOrder), eq(PaymentMethod.CASH), eq(orderTotal), eq(tenderedAmount), eq(null));
    }

    @Test
    @DisplayName("Should throw BusinessException when cash payment tendered amount is insufficient")
    void shouldThrowExceptionWhenCashPaymentTenderedAmountIsInsufficient() {
        ProcessCashPaymentRequest request = new ProcessCashPaymentRequest(new BigDecimal("15.00")); // Order total is 20.00

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(sampleOrder));

        assertThatThrownBy(() -> paymentApplicationService.processCashPayment(cafeId, orderId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tendered amount (15.00) cannot be less than order total amount");
    }

    @Test
    @DisplayName("Should process card payment with transaction reference")
    void shouldProcessCardPayment() {
        ProcessCardPaymentRequest request = new ProcessCardPaymentRequest("TXN-CARD-999");
        Money orderTotal = sampleOrder.getTotal();

        Payment payment = new Payment(paymentId, cafeId, orderId, PaymentMethod.CARD, orderTotal, null, null, "TXN-CARD-999");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        when(paymentDomainService.processPayment(eq(sampleOrder), eq(PaymentMethod.CARD), eq(orderTotal), eq(null), eq("TXN-CARD-999")))
                .thenReturn(payment);

        PaymentResponse response = paymentApplicationService.processCardPayment(cafeId, orderId, request);

        assertThat(response).isNotNull();
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(response.transactionReference()).isEqualTo("TXN-CARD-999");
    }

    @Test
    @DisplayName("Should generate UPI intent URI and Base64 QR code")
    void shouldGenerateUpiIntent() {
        String expectedUpiUri = "upi://pay?pa=cafe.pos%40upi&am=20.00";
        String expectedBase64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA";

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        when(upiQrGeneratorService.generateUpiUri(any(), any(), eq(sampleOrder.getTotal().amount()), any())).thenReturn(expectedUpiUri);
        when(upiQrGeneratorService.generateQrCodeBase64(expectedUpiUri)).thenReturn(expectedBase64);

        UpiIntentResponse response = paymentApplicationService.generateUpiIntent(cafeId, orderId);

        assertThat(response).isNotNull();
        assertThat(response.upiUri()).isEqualTo(expectedUpiUri);
        assertThat(response.qrCodeBase64()).isEqualTo(expectedBase64);
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    @DisplayName("Should process UPI confirmation payment")
    void shouldProcessUpiConfirmation() {
        ProcessUpiConfirmationRequest request = new ProcessUpiConfirmationRequest("UTR-12345678");
        Money orderTotal = sampleOrder.getTotal();

        Payment payment = new Payment(paymentId, cafeId, orderId, PaymentMethod.UPI_MANUAL, orderTotal, null, null, "UTR-12345678");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        when(paymentDomainService.processPayment(eq(sampleOrder), eq(PaymentMethod.UPI_MANUAL), eq(orderTotal), eq(null), eq("UTR-12345678")))
                .thenReturn(payment);

        PaymentResponse response = paymentApplicationService.processUpiConfirmation(cafeId, orderId, request);

        assertThat(response).isNotNull();
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.UPI_MANUAL);
        assertThat(response.transactionReference()).isEqualTo("UTR-12345678");
    }

    @Test
    @DisplayName("Should execute refund for completed payment")
    void shouldExecuteRefund() {
        RefundRequest request = new RefundRequest(new BigDecimal("20.00"), "Customer requested refund");
        Money paymentAmount = new Money(new BigDecimal("20.00"), sampleOrder.getCurrency());
        Payment payment = new Payment(paymentId, cafeId, orderId, PaymentMethod.CASH, paymentAmount, paymentAmount, null, null);
        Refund refund = new Refund(UUID.randomUUID(), cafeId, paymentId, paymentAmount, "Customer requested refund", employeeId);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentDomainService.processRefund(eq(payment), eq(paymentAmount), eq("Customer requested refund"), eq(employeeId)))
                .thenReturn(refund);

        RefundResponse response = paymentApplicationService.executeRefund(cafeId, paymentId, employeeId, request);

        assertThat(response).isNotNull();
        assertThat(response.paymentId()).isEqualTo(paymentId);
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(response.reason()).isEqualTo("Customer requested refund");
        assertThat(response.refundedByEmployeeId()).isEqualTo(employeeId);
    }

    @Test
    @DisplayName("Should throw BusinessException when refunding non-existent payment")
    void shouldThrowExceptionWhenRefundingNonExistentPayment() {
        RefundRequest request = new RefundRequest(new BigDecimal("20.00"), "Refund reason");

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentApplicationService.executeRefund(cafeId, paymentId, employeeId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Payment not found with id " + paymentId);
    }
}
