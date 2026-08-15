package com.cafepos.integration;

import com.cafepos.catalog.api.CreateCategoryRequest;
import com.cafepos.catalog.api.CreateProductRequest;
import com.cafepos.catalog.api.ProductResponse;
import com.cafepos.catalog.application.CategoryApplicationService;
import com.cafepos.catalog.application.ProductApplicationService;
import com.cafepos.catalog.domain.UnitOfMeasure;
import com.cafepos.ordering.api.AddOrderLineRequest;
import com.cafepos.ordering.api.CreateOrderRequest;
import com.cafepos.ordering.api.OrderResponse;
import com.cafepos.ordering.application.OrderApplicationService;
import com.cafepos.ordering.domain.OrderType;
import com.cafepos.organization.application.OnboardCafeCommand;
import com.cafepos.organization.application.OnboardCafeResponse;
import com.cafepos.organization.application.OnboardCafeUseCase;
import com.cafepos.payment.api.PaymentResponse;
import com.cafepos.payment.api.ProcessCashPaymentRequest;
import com.cafepos.payment.application.PaymentApplicationService;
import com.cafepos.payment.domain.PaymentMethod;
import com.cafepos.payment.domain.PaymentStatus;
import com.cafepos.session.api.OpenSessionRequest;
import com.cafepos.session.api.PosSessionResponse;
import com.cafepos.session.application.PosSessionApplicationService;
import com.cafepos.session.domain.PosSessionStatus;
import com.cafepos.session.domain.Terminal;
import com.cafepos.session.domain.TerminalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = ".*neon\\.tech.*")
@Transactional
public class NeonPostgresIntegrationIT {

    @Autowired
    private OnboardCafeUseCase onboardCafeUseCase;

    @Autowired
    private CategoryApplicationService categoryApplicationService;

    @Autowired
    private ProductApplicationService productApplicationService;

    @Autowired
    private TerminalRepository terminalRepository;

    @Autowired
    private PosSessionApplicationService posSessionApplicationService;

    @Autowired
    private OrderApplicationService orderApplicationService;

    @Autowired
    private PaymentApplicationService paymentApplicationService;

    private UUID cafeId;
    private UUID adminEmployeeId;

    @BeforeEach
    void setUp() {
        String uniqueSlug = "test-cafe-" + UUID.randomUUID().toString().substring(0, 8);
        OnboardCafeCommand onboardCmd = new OnboardCafeCommand(
                "Neon Test Cafe",
                uniqueSlug,
                "UTC",
                "USD",
                "Integration Admin",
                "admin@" + uniqueSlug + ".com",
                "Password123!"
        );
        OnboardCafeResponse onboardResponse = onboardCafeUseCase.execute(onboardCmd);
        cafeId = onboardResponse.cafeId();
        adminEmployeeId = onboardResponse.employeeId();
    }

    @Test
    @DisplayName("Complete end-to-end POS workflow against Neon PostgreSQL test database")
    void testCompletePosWorkflowAgainstNeonPostgres() {
        // 1. Create Category & Product
        var categoryResp = categoryApplicationService.createCategory(cafeId, new CreateCategoryRequest("Coffee", "#8B4513", 1));
        
        CreateProductRequest productReq = new CreateProductRequest(
                categoryResp.id(),
                "Espresso Single",
                "ESP-" + UUID.randomUUID().toString().substring(0, 4),
                new BigDecimal("3.50"),
                "USD",
                UnitOfMeasure.UNIT,
                false
        );
        ProductResponse productResp = productApplicationService.createProduct(cafeId, productReq);
        assertThat(productResp.name()).isEqualTo("Espresso Single");

        // 2. Setup Terminal & Open POS Session
        Terminal terminal = terminalRepository.save(new Terminal(cafeId, "Terminal 1", "T1-" + UUID.randomUUID().toString().substring(0, 4)));
        PosSessionResponse sessionResp = posSessionApplicationService.openSession(
                cafeId,
                adminEmployeeId,
                new OpenSessionRequest(terminal.getId(), new BigDecimal("50.00"))
        );
        assertThat(sessionResp.status()).isEqualTo(PosSessionStatus.OPEN);

        // 3. Create Order & Add Lines
        CreateOrderRequest createOrderReq = new CreateOrderRequest(
                sessionResp.id(),
                OrderType.TAKEAWAY,
                null,
                "Takeaway Order",
                "USD"
        );
        OrderResponse orderResp = orderApplicationService.createOrder(cafeId, createOrderReq);
        
        OrderResponse updatedOrder = orderApplicationService.addOrderLine(
                cafeId,
                orderResp.id(),
                new AddOrderLineRequest(productResp.id(), 2)
        );
        assertThat(updatedOrder.totalAmount()).isEqualByComparingTo("7.00");

        // 4. Process Cash Payment
        PaymentResponse paymentResp = paymentApplicationService.processCashPayment(
                cafeId,
                orderResp.id(),
                new ProcessCashPaymentRequest(new BigDecimal("10.00"))
        );
        assertThat(paymentResp.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(paymentResp.paymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(paymentResp.changeAmount()).isEqualByComparingTo("3.00");
    }
}
