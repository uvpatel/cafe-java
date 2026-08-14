package com.cafepos.payment.application;

import com.cafepos.ordering.domain.Order;
import com.cafepos.ordering.domain.OrderRepository;
import com.cafepos.ordering.domain.OrderStatus;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Service
public class PaymentApplicationService {

    private static final String DEFAULT_PAYEE_VPA = "cafe.pos@upi";
    private static final String DEFAULT_PAYEE_NAME = "Odoo Cafe POS";

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentDomainService paymentDomainService;
    private final UpiQrGeneratorService upiQrGeneratorService;
    private final PaymentDtoMapper paymentDtoMapper;

    public PaymentApplicationService(OrderRepository orderRepository,
                                     PaymentRepository paymentRepository,
                                     PaymentDomainService paymentDomainService,
                                     UpiQrGeneratorService upiQrGeneratorService,
                                     PaymentDtoMapper paymentDtoMapper) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.paymentDomainService = Objects.requireNonNull(paymentDomainService, "paymentDomainService must not be null");
        this.upiQrGeneratorService = Objects.requireNonNull(upiQrGeneratorService, "upiQrGeneratorService must not be null");
        this.paymentDtoMapper = Objects.requireNonNull(paymentDtoMapper, "paymentDtoMapper must not be null");
    }

    @Transactional
    public PaymentResponse processCashPayment(UUID cafeId, UUID orderId, ProcessCashPaymentRequest request) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        Order order = findAndValidateOrder(cafeId, orderId);

        Money orderTotal = order.getTotal();
        Money tenderedMoney = new Money(request.tenderedAmount(), order.getCurrency());

        if (tenderedMoney.amount().compareTo(orderTotal.amount()) < 0) {
            throw new BusinessException("INSUFFICIENT_TENDER", "Tendered amount (" + request.tenderedAmount()
                    + ") cannot be less than order total amount (" + orderTotal.amount() + ")", 400);
        }

        try {
            Payment payment = paymentDomainService.processPayment(
                    order,
                    PaymentMethod.CASH,
                    orderTotal,
                    tenderedMoney,
                    null
            );
            return paymentDtoMapper.toPaymentResponse(payment);
        } catch (IllegalStateException e) {
            throw new BusinessException("PAYMENT_ALREADY_EXISTS", e.getMessage(), 400);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_PAYMENT_REQUEST", e.getMessage(), 400);
        }
    }

    @Transactional
    public PaymentResponse processCardPayment(UUID cafeId, UUID orderId, ProcessCardPaymentRequest request) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");

        Order order = findAndValidateOrder(cafeId, orderId);
        String txRef = request != null ? request.transactionReference() : null;

        try {
            Payment payment = paymentDomainService.processPayment(
                    order,
                    PaymentMethod.CARD,
                    order.getTotal(),
                    null,
                    txRef
            );
            return paymentDtoMapper.toPaymentResponse(payment);
        } catch (IllegalStateException e) {
            throw new BusinessException("PAYMENT_ALREADY_EXISTS", e.getMessage(), 400);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_PAYMENT_REQUEST", e.getMessage(), 400);
        }
    }

    @Transactional(readOnly = true)
    public UpiIntentResponse generateUpiIntent(UUID cafeId, UUID orderId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");

        Order order = findAndValidateOrderForUpiIntent(cafeId, orderId);

        BigDecimal totalAmount = order.getTotal().amount();
        String transactionNote = "Order #" + order.getOrderNumber();

        String upiUri = upiQrGeneratorService.generateUpiUri(
                DEFAULT_PAYEE_VPA,
                DEFAULT_PAYEE_NAME,
                totalAmount,
                transactionNote
        );

        String qrCodeBase64 = upiQrGeneratorService.generateQrCodeBase64(upiUri);

        return new UpiIntentResponse(
                upiUri,
                qrCodeBase64,
                totalAmount,
                DEFAULT_PAYEE_VPA,
                DEFAULT_PAYEE_NAME,
                transactionNote
        );
    }

    @Transactional
    public PaymentResponse processUpiConfirmation(UUID cafeId, UUID orderId, ProcessUpiConfirmationRequest request) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");

        Order order = findAndValidateOrder(cafeId, orderId);
        String txRef = request != null ? request.transactionReference() : null;

        try {
            Payment payment = paymentDomainService.processPayment(
                    order,
                    PaymentMethod.UPI_MANUAL,
                    order.getTotal(),
                    null,
                    txRef
            );
            return paymentDtoMapper.toPaymentResponse(payment);
        } catch (IllegalStateException e) {
            throw new BusinessException("PAYMENT_ALREADY_EXISTS", e.getMessage(), 400);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_PAYMENT_REQUEST", e.getMessage(), 400);
        }
    }

    @Transactional
    public RefundResponse executeRefund(UUID cafeId, UUID paymentId, UUID employeeId, RefundRequest request) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(employeeId, "employeeId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND", "Payment not found with id " + paymentId, 404));

        if (!payment.getCafeId().equals(cafeId)) {
            throw new BusinessException("PAYMENT_NOT_FOUND", "Payment not found with id " + paymentId, 404);
        }

        Money refundAmount = new Money(request.amount(), payment.getCurrency());

        try {
            Refund refund = paymentDomainService.processRefund(payment, refundAmount, request.reason(), employeeId);
            return paymentDtoMapper.toRefundResponse(refund, payment.getCurrency().getCurrencyCode());
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new BusinessException("REFUND_FAILED", e.getMessage(), 400);
        }
    }

    private Order findAndValidateOrder(UUID cafeId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found with id " + orderId, 404));

        if (!order.getCafeId().equals(cafeId)) {
            throw new BusinessException("ORDER_NOT_FOUND", "Order not found with id " + orderId, 404);
        }

        if (order.getStatus() == OrderStatus.PAID) {
            throw new BusinessException("ORDER_ALREADY_PAID", "Order is already paid", 400);
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("ORDER_CANCELLED", "Cannot pay for a cancelled order", 400);
        }

        return order;
    }

    private Order findAndValidateOrderForUpiIntent(UUID cafeId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found with id " + orderId, 404));

        if (!order.getCafeId().equals(cafeId)) {
            throw new BusinessException("ORDER_NOT_FOUND", "Order not found with id " + orderId, 404);
        }

        if (order.getStatus() == OrderStatus.PAID) {
            throw new BusinessException("ORDER_ALREADY_PAID", "Order is already paid", 400);
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("ORDER_CANCELLED", "Cannot generate UPI intent for a cancelled order", 400);
        }

        return order;
    }
}
