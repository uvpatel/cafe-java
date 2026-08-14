package com.cafepos.e2e;

import com.cafepos.activitylog.application.ActivityLogApplicationService;
import com.cafepos.activitylog.application.AuditEventListener;
import com.cafepos.activitylog.domain.ActivityLog;
import com.cafepos.activitylog.domain.ActivityLogRepository;
import com.cafepos.activitylog.domain.events.PaymentProcessedEvent;
import com.cafepos.activitylog.domain.events.PosSessionClosedEvent;
import com.cafepos.activitylog.domain.events.PosSessionOpenedEvent;
import com.cafepos.activitylog.domain.events.UserOnboardedEvent;
import com.cafepos.catalog.api.CatalogDtoMapper;
import com.cafepos.catalog.api.CategoryResponse;
import com.cafepos.catalog.api.CreateCategoryRequest;
import com.cafepos.catalog.api.CreateProductRequest;
import com.cafepos.catalog.api.ProductResponse;
import com.cafepos.catalog.application.CategoryApplicationService;
import com.cafepos.catalog.application.ProductApplicationService;
import com.cafepos.catalog.domain.CategoryValidationService;
import com.cafepos.catalog.domain.Product;
import com.cafepos.catalog.domain.ProductCategory;
import com.cafepos.catalog.domain.ProductCategoryRepository;
import com.cafepos.catalog.domain.ProductPricingValidator;
import com.cafepos.catalog.domain.ProductRepository;
import com.cafepos.catalog.domain.UnitOfMeasure;
import com.cafepos.floor.domain.DiningTable;
import com.cafepos.floor.domain.DiningTableRepository;
import com.cafepos.floor.domain.Floor;
import com.cafepos.floor.domain.FloorRepository;
import com.cafepos.identity.application.AuthenticateUserUseCase;
import com.cafepos.identity.application.LoginCommand;
import com.cafepos.identity.application.LoginResponse;
import com.cafepos.identity.domain.Employee;
import com.cafepos.identity.domain.EmployeeRepository;
import com.cafepos.identity.domain.EmployeeRole;
import com.cafepos.identity.domain.UserAccount;
import com.cafepos.identity.domain.UserAccountRepository;
import com.cafepos.identity.infrastructure.BCryptPasswordEncodingService;
import com.cafepos.identity.infrastructure.NimbusJwtTokenService;
import com.cafepos.identity.infrastructure.PasswordEncodingService;
import com.cafepos.kitchen.api.DispatchOrderToKitchenResponse;
import com.cafepos.kitchen.api.KitchenDtoMapper;
import com.cafepos.kitchen.api.KitchenTicketResponse;
import com.cafepos.kitchen.application.KitchenApplicationService;
import com.cafepos.kitchen.domain.KitchenDispatchService;
import com.cafepos.kitchen.domain.KitchenItemStatus;
import com.cafepos.kitchen.domain.KitchenTicket;
import com.cafepos.kitchen.domain.KitchenTicketDispatchedEvent;
import com.cafepos.kitchen.domain.KitchenTicketItem;
import com.cafepos.kitchen.domain.KitchenTicketItemRepository;
import com.cafepos.kitchen.domain.KitchenTicketRepository;
import com.cafepos.kitchen.domain.KitchenTicketStatus;
import com.cafepos.ordering.api.AddOrderLineRequest;
import com.cafepos.ordering.api.CreateOrderRequest;
import com.cafepos.ordering.api.OrderDtoMapper;
import com.cafepos.ordering.api.OrderResponse;
import com.cafepos.ordering.application.OrderApplicationService;
import com.cafepos.ordering.domain.Order;
import com.cafepos.ordering.domain.OrderNumberGenerator;
import com.cafepos.ordering.domain.OrderRepository;
import com.cafepos.ordering.domain.OrderStatus;
import com.cafepos.ordering.domain.OrderType;
import com.cafepos.organization.application.OnboardCafeCommand;
import com.cafepos.organization.application.OnboardCafeResponse;
import com.cafepos.organization.application.OnboardCafeUseCase;
import com.cafepos.organization.domain.Cafe;
import com.cafepos.organization.domain.CafeRepository;
import com.cafepos.payment.api.PaymentDtoMapper;
import com.cafepos.payment.api.PaymentResponse;
import com.cafepos.payment.api.ProcessCashPaymentRequest;
import com.cafepos.payment.application.PaymentApplicationService;
import com.cafepos.payment.domain.Payment;
import com.cafepos.payment.domain.PaymentDomainService;
import com.cafepos.payment.domain.PaymentMethod;
import com.cafepos.payment.domain.PaymentRepository;
import com.cafepos.payment.domain.PaymentStatus;
import com.cafepos.payment.domain.Refund;
import com.cafepos.payment.domain.RefundRepository;
import com.cafepos.payment.infrastructure.UpiQrGeneratorService;
import com.cafepos.receipt.api.ReceiptDtoMapper;
import com.cafepos.receipt.api.ReceiptResponse;
import com.cafepos.receipt.application.ReceiptApplicationService;
import com.cafepos.reporting.application.ExportJobRunner;
import com.cafepos.reporting.application.ReportingApplicationService;
import com.cafepos.reporting.application.SalesSummaryProjection;
import com.cafepos.reporting.domain.ExportJobRepository;
import com.cafepos.session.api.CloseSessionRequest;
import com.cafepos.session.api.OpenSessionRequest;
import com.cafepos.session.api.PosSessionDtoMapper;
import com.cafepos.session.api.PosSessionResponse;
import com.cafepos.session.api.RecordCashMovementRequest;
import com.cafepos.session.application.PosSessionApplicationService;
import com.cafepos.session.domain.CashMovement;
import com.cafepos.session.domain.CashMovementRepository;
import com.cafepos.session.domain.CashMovementType;
import com.cafepos.session.domain.PosSession;
import com.cafepos.session.domain.PosSessionRepository;
import com.cafepos.session.domain.PosSessionStatus;
import com.cafepos.session.domain.Terminal;
import com.cafepos.session.domain.TerminalRepository;
import com.cafepos.shared.error.BusinessException;
import com.cafepos.shared.events.OutboxEvent;
import com.cafepos.shared.events.OutboxEventRepository;
import com.cafepos.shared.events.OutboxEventStatus;
import com.cafepos.shared.events.OutboxPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class FirstSaleVerticalSliceE2ETest {

    // Domain Repositories (In-Memory backed via Mockito or Maps)
    private CafeRepository cafeRepository;
    private EmployeeRepository employeeRepository;
    private UserAccountRepository userAccountRepository;
    private ProductCategoryRepository categoryRepository;
    private ProductRepository productRepository;
    private FloorRepository floorRepository;
    private DiningTableRepository diningTableRepository;
    private TerminalRepository terminalRepository;
    private PosSessionRepository posSessionRepository;
    private CashMovementRepository cashMovementRepository;
    private OrderRepository orderRepository;
    private KitchenTicketRepository kitchenTicketRepository;
    private KitchenTicketItemRepository kitchenTicketItemRepository;
    private OutboxEventRepository outboxEventRepository;
    private PaymentRepository paymentRepository;
    private RefundRepository refundRepository;
    private ActivityLogRepository activityLogRepository;
    private ExportJobRepository exportJobRepository;
    private SimpMessagingTemplate messagingTemplate;
    private ExportJobRunner exportJobRunner;

    // Infrastructure Services
    private PasswordEncodingService passwordEncodingService;
    private NimbusJwtTokenService jwtTokenService;

    // Application Use Cases & Services
    private OnboardCafeUseCase onboardCafeUseCase;
    private AuthenticateUserUseCase authenticateUserUseCase;
    private CategoryApplicationService categoryApplicationService;
    private ProductApplicationService productApplicationService;
    private PosSessionApplicationService posSessionApplicationService;
    private OrderApplicationService orderApplicationService;
    private KitchenDispatchService kitchenDispatchService;
    private OutboxPublisher outboxPublisher;
    private KitchenApplicationService kitchenApplicationService;
    private PaymentApplicationService paymentApplicationService;
    private ReceiptApplicationService receiptApplicationService;
    private ActivityLogApplicationService activityLogApplicationService;
    private AuditEventListener auditEventListener;
    private ReportingApplicationService reportingApplicationService;

    // In-Memory storage maps
    private final Map<UUID, Cafe> cafes = new ConcurrentHashMap<>();
    private final Map<UUID, Employee> employees = new ConcurrentHashMap<>();
    private final Map<UUID, UserAccount> userAccounts = new ConcurrentHashMap<>();
    private final Map<UUID, ProductCategory> categories = new ConcurrentHashMap<>();
    private final Map<UUID, Product> products = new ConcurrentHashMap<>();
    private final Map<UUID, Floor> floors = new ConcurrentHashMap<>();
    private final Map<UUID, DiningTable> diningTables = new ConcurrentHashMap<>();
    private final Map<UUID, Terminal> terminals = new ConcurrentHashMap<>();
    private final Map<UUID, PosSession> posSessions = new ConcurrentHashMap<>();
    private final List<CashMovement> cashMovements = Collections.synchronizedList(new ArrayList<>());
    private final Map<UUID, Order> orders = new ConcurrentHashMap<>();
    private final Map<UUID, KitchenTicket> kitchenTickets = new ConcurrentHashMap<>();
    private final Map<UUID, KitchenTicketItem> kitchenTicketItems = new ConcurrentHashMap<>();
    private final Map<UUID, OutboxEvent> outboxEvents = new ConcurrentHashMap<>();
    private final Map<UUID, Payment> payments = new ConcurrentHashMap<>();
    private final Map<UUID, Refund> refunds = new ConcurrentHashMap<>();
    private final List<ActivityLog> activityLogs = Collections.synchronizedList(new ArrayList<>());

    @BeforeEach
    void setUp() {
        cafes.clear();
        employees.clear();
        userAccounts.clear();
        categories.clear();
        products.clear();
        floors.clear();
        diningTables.clear();
        terminals.clear();
        posSessions.clear();
        cashMovements.clear();
        orders.clear();
        kitchenTickets.clear();
        kitchenTicketItems.clear();
        outboxEvents.clear();
        payments.clear();
        refunds.clear();
        activityLogs.clear();

        // 1. Setup CafeRepository
        cafeRepository = Mockito.mock(CafeRepository.class);
        when(cafeRepository.save(any(Cafe.class))).thenAnswer(invocation -> {
            Cafe c = invocation.getArgument(0);
            cafes.put(c.getId(), c);
            return c;
        });
        when(cafeRepository.existsBySlug(anyString())).thenAnswer(invocation -> {
            String slug = invocation.getArgument(0);
            return cafes.values().stream().anyMatch(c -> c.getSlug().equalsIgnoreCase(slug));
        });
        when(cafeRepository.findById(any(UUID.class))).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            return Optional.ofNullable(cafes.get(id));
        });

        // 2. Setup EmployeeRepository
        employeeRepository = Mockito.mock(EmployeeRepository.class);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee e = invocation.getArgument(0);
            employees.put(e.getId(), e);
            return e;
        });
        when(employeeRepository.findByCafeIdAndEmail(any(UUID.class), anyString())).thenAnswer(invocation -> {
            UUID cafeId = invocation.getArgument(0);
            String email = invocation.getArgument(1);
            return employees.values().stream()
                    .filter(e -> e.getCafeId().equals(cafeId) && e.getEmail().equalsIgnoreCase(email))
                    .findFirst();
        });
        when(employeeRepository.findByEmailIgnoreCase(anyString())).thenAnswer(invocation -> {
            String email = invocation.getArgument(0);
            return employees.values().stream()
                    .filter(e -> e.getEmail().equalsIgnoreCase(email))
                    .findFirst();
        });
        when(employeeRepository.findById(any(UUID.class))).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            return Optional.ofNullable(employees.get(id));
        });

        // 3. Setup UserAccountRepository
        userAccountRepository = Mockito.mock(UserAccountRepository.class);
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount ua = invocation.getArgument(0);
            userAccounts.put(ua.getId(), ua);
            return ua;
        });
        when(userAccountRepository.findByEmployeeId(any(UUID.class))).thenAnswer(invocation -> {
            UUID empId = invocation.getArgument(0);
            return userAccounts.values().stream()
                    .filter(ua -> ua.getEmployeeId().equals(empId))
                    .findFirst();
        });

        // 4. Setup ProductCategoryRepository
        categoryRepository = Mockito.mock(ProductCategoryRepository.class);
        when(categoryRepository.save(any(ProductCategory.class))).thenAnswer(invocation -> {
            ProductCategory pc = invocation.getArgument(0);
            categories.put(pc.getId(), pc);
            return pc;
        });
        when(categoryRepository.findByIdAndCafeId(any(UUID.class), any(UUID.class))).thenAnswer(invocation -> {
            UUID catId = invocation.getArgument(0);
            UUID cafeId = invocation.getArgument(1);
            ProductCategory pc = categories.get(catId);
            return (pc != null && pc.getCafeId().equals(cafeId)) ? Optional.of(pc) : Optional.empty();
        });
        when(categoryRepository.existsByCafeIdAndNameIgnoreCase(any(UUID.class), anyString())).thenAnswer(invocation -> {
            UUID cafeId = invocation.getArgument(0);
            String name = invocation.getArgument(1);
            return categories.values().stream()
                    .anyMatch(c -> c.getCafeId().equals(cafeId) && c.getName().equalsIgnoreCase(name));
        });
        when(categoryRepository.findByCafeIdOrderBySortOrderAsc(any(UUID.class))).thenAnswer(invocation -> {
            UUID cafeId = invocation.getArgument(0);
            return categories.values().stream()
                    .filter(c -> c.getCafeId().equals(cafeId))
                    .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                    .toList();
        });
        when(categoryRepository.findByCafeIdAndArchivedFalseOrderBySortOrderAsc(any(UUID.class))).thenAnswer(invocation -> {
            UUID cafeId = invocation.getArgument(0);
            return categories.values().stream()
                    .filter(c -> c.getCafeId().equals(cafeId) && !c.isArchived())
                    .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                    .toList();
        });

        // 5. Setup ProductRepository
        productRepository = Mockito.mock(ProductRepository.class);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            products.put(p.getId(), p);
            return p;
        });
        when(productRepository.findByIdAndCafeId(any(UUID.class), any(UUID.class))).thenAnswer(invocation -> {
            UUID pId = invocation.getArgument(0);
            UUID cafeId = invocation.getArgument(1);
            Product p = products.get(pId);
            return (p != null && p.getCafeId().equals(cafeId)) ? Optional.of(p) : Optional.empty();
        });
        when(productRepository.existsByCafeIdAndSku(any(UUID.class), anyString())).thenAnswer(invocation -> {
            UUID cafeId = invocation.getArgument(0);
            String sku = invocation.getArgument(1);
            return products.values().stream()
                    .anyMatch(p -> p.getCafeId().equals(cafeId) && p.getSku() != null && p.getSku().equalsIgnoreCase(sku));
        });
        when(productRepository.findByCafeId(any(UUID.class))).thenAnswer(invocation -> {
            UUID cafeId = invocation.getArgument(0);
            return products.values().stream().filter(p -> p.getCafeId().equals(cafeId)).toList();
        });
        when(productRepository.findByCafeIdAndArchivedFalse(any(UUID.class))).thenAnswer(invocation -> {
            UUID cafeId = invocation.getArgument(0);
            return products.values().stream().filter(p -> p.getCafeId().equals(cafeId) && !p.isArchived()).toList();
        });

        // 6. Setup FloorRepository & DiningTableRepository
        floorRepository = Mockito.mock(FloorRepository.class);
        when(floorRepository.save(any(Floor.class))).thenAnswer(invocation -> {
            Floor f = invocation.getArgument(0);
            floors.put(f.getId(), f);
            return f;
        });

        diningTableRepository = Mockito.mock(DiningTableRepository.class);
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> {
            DiningTable dt = invocation.getArgument(0);
            diningTables.put(dt.getId(), dt);
            return dt;
        });
        when(diningTableRepository.findByIdAndCafeId(any(UUID.class), any(UUID.class))).thenAnswer(invocation -> {
            UUID dtId = invocation.getArgument(0);
            UUID cafeId = invocation.getArgument(1);
            DiningTable dt = diningTables.get(dtId);
            return (dt != null && dt.getCafeId().equals(cafeId)) ? Optional.of(dt) : Optional.empty();
        });

        // 7. Setup TerminalRepository
        terminalRepository = Mockito.mock(TerminalRepository.class);
        when(terminalRepository.save(any(Terminal.class))).thenAnswer(invocation -> {
            Terminal t = invocation.getArgument(0);
            terminals.put(t.getId(), t);
            return t;
        });
        when(terminalRepository.findByIdAndCafeId(any(UUID.class), any(UUID.class))).thenAnswer(invocation -> {
            UUID tId = invocation.getArgument(0);
            UUID cafeId = invocation.getArgument(1);
            Terminal t = terminals.get(tId);
            return (t != null && t.getCafeId().equals(cafeId)) ? Optional.of(t) : Optional.empty();
        });

        // 8. Setup PosSessionRepository & CashMovementRepository
        posSessionRepository = Mockito.mock(PosSessionRepository.class);
        when(posSessionRepository.save(any(PosSession.class))).thenAnswer(invocation -> {
            PosSession ps = invocation.getArgument(0);
            posSessions.put(ps.getId(), ps);
            return ps;
        });
        when(posSessionRepository.findByIdAndCafeId(any(UUID.class), any(UUID.class))).thenAnswer(invocation -> {
            UUID sId = invocation.getArgument(0);
            UUID cafeId = invocation.getArgument(1);
            PosSession ps = posSessions.get(sId);
            return (ps != null && ps.getCafeId().equals(cafeId)) ? Optional.of(ps) : Optional.empty();
        });
        when(posSessionRepository.existsByCafeIdAndTerminalIdAndStatus(any(UUID.class), any(UUID.class), any(PosSessionStatus.class))).thenAnswer(invocation -> {
            UUID cafeId = invocation.getArgument(0);
            UUID termId = invocation.getArgument(1);
            PosSessionStatus st = invocation.getArgument(2);
            return posSessions.values().stream()
                    .anyMatch(s -> s.getCafeId().equals(cafeId) && s.getTerminalId().equals(termId) && s.getStatus() == st);
        });
        when(posSessionRepository.findByCafeId(any(UUID.class))).thenAnswer(invocation -> {
            UUID cafeId = invocation.getArgument(0);
            return posSessions.values().stream().filter(s -> s.getCafeId().equals(cafeId)).toList();
        });

        cashMovementRepository = Mockito.mock(CashMovementRepository.class);
        when(cashMovementRepository.save(any(CashMovement.class))).thenAnswer(invocation -> {
            CashMovement cm = invocation.getArgument(0);
            cashMovements.add(cm);
            return cm;
        });
        when(cashMovementRepository.findByCafeIdAndSessionId(any(UUID.class), any(UUID.class))).thenAnswer(invocation -> {
            UUID cafeId = invocation.getArgument(0);
            UUID sessionId = invocation.getArgument(1);
            return cashMovements.stream()
                    .filter(cm -> cm.getCafeId().equals(cafeId) && cm.getSessionId().equals(sessionId))
                    .toList();
        });

        // 9. Setup OrderRepository
        orderRepository = Mockito.mock(OrderRepository.class);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            orders.put(o.getId(), o);
            return o;
        });
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            orders.put(o.getId(), o);
            return o;
        });
        when(orderRepository.findById(any(UUID.class))).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            return Optional.ofNullable(orders.get(id));
        });
        when(orderRepository.findByIdAndCafeId(any(UUID.class), any(UUID.class))).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            UUID cafeId = invocation.getArgument(1);
            Order o = orders.get(id);
            return (o != null && o.getCafeId().equals(cafeId)) ? Optional.of(o) : Optional.empty();
        });
        when(orderRepository.findByCafeId(any(UUID.class))).thenAnswer(invocation -> {
            UUID cafeId = invocation.getArgument(0);
            return orders.values().stream().filter(o -> o.getCafeId().equals(cafeId)).toList();
        });
        when(orderRepository.findByCafeIdAndDiningTableIdAndStatusIn(any(UUID.class), any(UUID.class), any())).thenAnswer(invocation -> {
            UUID cafeId = invocation.getArgument(0);
            UUID dtId = invocation.getArgument(1);
            List<OrderStatus> statuses = invocation.getArgument(2);
            return orders.values().stream()
                    .filter(o -> o.getCafeId().equals(cafeId) && dtId.equals(o.getDiningTableId()) && statuses.contains(o.getStatus()))
                    .findFirst();
        });
        when(orderRepository.existsByCafeIdAndOrderNumberIgnoreCase(any(UUID.class), anyString())).thenAnswer(invocation -> {
            UUID cafeId = invocation.getArgument(0);
            String orderNum = invocation.getArgument(1);
            return orders.values().stream()
                    .anyMatch(o -> o.getCafeId().equals(cafeId) && o.getOrderNumber().equalsIgnoreCase(orderNum));
        });

        // 10. Setup KitchenTicketRepository & KitchenTicketItemRepository
        kitchenTicketRepository = Mockito.mock(KitchenTicketRepository.class);
        when(kitchenTicketRepository.save(any(KitchenTicket.class))).thenAnswer(invocation -> {
            KitchenTicket kt = invocation.getArgument(0);
            kitchenTickets.put(kt.getId(), kt);
            for (KitchenTicketItem item : kt.getItems()) {
                kitchenTicketItems.put(item.getId(), item);
            }
            return kt;
        });
        when(kitchenTicketRepository.findByOrderId(any(UUID.class))).thenAnswer(invocation -> {
            UUID orderId = invocation.getArgument(0);
            return kitchenTickets.values().stream()
                    .filter(kt -> kt.getOrderId().equals(orderId))
                    .findFirst();
        });
        when(kitchenTicketRepository.findByIdAndCafeId(any(UUID.class), any(UUID.class))).thenAnswer(invocation -> {
            UUID tId = invocation.getArgument(0);
            UUID cafeId = invocation.getArgument(1);
            KitchenTicket kt = kitchenTickets.get(tId);
            return (kt != null && kt.getCafeId().equals(cafeId)) ? Optional.of(kt) : Optional.empty();
        });
        when(kitchenTicketRepository.findByCafeId(any(UUID.class))).thenAnswer(invocation -> {
            UUID cafeId = invocation.getArgument(0);
            return kitchenTickets.values().stream().filter(kt -> kt.getCafeId().equals(cafeId)).toList();
        });

        kitchenTicketItemRepository = Mockito.mock(KitchenTicketItemRepository.class);
        when(kitchenTicketItemRepository.findById(any(UUID.class))).thenAnswer(invocation -> {
            UUID itemId = invocation.getArgument(0);
            return Optional.ofNullable(kitchenTicketItems.get(itemId));
        });

        // 11. Setup OutboxEventRepository
        outboxEventRepository = Mockito.mock(OutboxEventRepository.class);
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> {
            OutboxEvent oe = invocation.getArgument(0);
            outboxEvents.put(oe.getId(), oe);
            return oe;
        });
        when(outboxEventRepository.findByStatus(any(OutboxEventStatus.class))).thenAnswer(invocation -> {
            OutboxEventStatus st = invocation.getArgument(0);
            return outboxEvents.values().stream().filter(oe -> oe.getStatus() == st).toList();
        });

        // 12. Setup PaymentRepository & RefundRepository
        paymentRepository = Mockito.mock(PaymentRepository.class);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            payments.put(p.getId(), p);
            return p;
        });
        when(paymentRepository.findByOrderId(any(UUID.class))).thenAnswer(invocation -> {
            UUID orderId = invocation.getArgument(0);
            return payments.values().stream().filter(p -> p.getOrderId().equals(orderId)).findFirst();
        });
        when(paymentRepository.findByCafeId(any(UUID.class))).thenAnswer(invocation -> {
            UUID cafeId = invocation.getArgument(0);
            return payments.values().stream().filter(p -> p.getCafeId().equals(cafeId)).toList();
        });
        when(paymentRepository.findById(any(UUID.class))).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            return Optional.ofNullable(payments.get(id));
        });

        refundRepository = Mockito.mock(RefundRepository.class);
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> {
            Refund r = invocation.getArgument(0);
            refunds.put(r.getId(), r);
            return r;
        });

        // 13. Setup ActivityLogRepository
        activityLogRepository = Mockito.mock(ActivityLogRepository.class);
        when(activityLogRepository.save(any(ActivityLog.class))).thenAnswer(invocation -> {
            ActivityLog al = invocation.getArgument(0);
            activityLogs.add(al);
            return al;
        });
        when(activityLogRepository.findByCafeIdOrderByCreatedAtDesc(any(UUID.class), any(Pageable.class))).thenAnswer(invocation -> {
            UUID cafeId = invocation.getArgument(0);
            List<ActivityLog> filtered = activityLogs.stream()
                    .filter(al -> al.getCafeId().equals(cafeId))
                    .toList();
            return new PageImpl<>(filtered);
        });

        // 14. Infrastructure & Services Wiring
        passwordEncodingService = new BCryptPasswordEncodingService();
        jwtTokenService = new NimbusJwtTokenService(
                "super-secret-jwt-signing-key-for-testing-cafepos-backend-e2e-vertical-slice-verify-key-0123456789",
                3600L,
                604800L
        );
        messagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
        exportJobRepository = Mockito.mock(ExportJobRepository.class);
        exportJobRunner = Mockito.mock(ExportJobRunner.class);

        onboardCafeUseCase = new OnboardCafeUseCase(cafeRepository, employeeRepository, userAccountRepository, passwordEncodingService);
        authenticateUserUseCase = new AuthenticateUserUseCase(employeeRepository, userAccountRepository, passwordEncodingService, jwtTokenService);

        CategoryValidationService categoryValidationService = new CategoryValidationService(categoryRepository);
        CatalogDtoMapper catalogDtoMapper = new CatalogDtoMapper();
        categoryApplicationService = new CategoryApplicationService(categoryRepository, categoryValidationService, catalogDtoMapper);

        ProductPricingValidator productPricingValidator = new ProductPricingValidator();
        productApplicationService = new ProductApplicationService(productRepository, categoryRepository, productPricingValidator, catalogDtoMapper);

        PosSessionDtoMapper posSessionDtoMapper = new PosSessionDtoMapper();
        posSessionApplicationService = new PosSessionApplicationService(posSessionRepository, cashMovementRepository, terminalRepository, posSessionDtoMapper);

        OrderNumberGenerator orderNumberGenerator = new OrderNumberGenerator(orderRepository);
        OrderDtoMapper orderDtoMapper = new OrderDtoMapper();
        orderApplicationService = new OrderApplicationService(orderRepository, posSessionRepository, diningTableRepository, productRepository, orderNumberGenerator, orderDtoMapper);

        KitchenDtoMapper kitchenDtoMapper = new KitchenDtoMapper();
        ObjectMapper objectMapper = new ObjectMapper();
        kitchenDispatchService = new KitchenDispatchService(kitchenTicketRepository, outboxEventRepository, objectMapper);
        outboxPublisher = new OutboxPublisher(outboxEventRepository, messagingTemplate, objectMapper);
        kitchenApplicationService = new KitchenApplicationService(orderRepository, kitchenTicketRepository, kitchenTicketItemRepository, kitchenDispatchService, kitchenDtoMapper);

        PaymentDtoMapper paymentDtoMapper = new PaymentDtoMapper();
        PaymentDomainService paymentDomainService = new PaymentDomainService(paymentRepository, refundRepository, orderRepository);
        UpiQrGeneratorService upiQrGeneratorService = new UpiQrGeneratorService();
        paymentApplicationService = new PaymentApplicationService(orderRepository, paymentRepository, paymentDomainService, upiQrGeneratorService, paymentDtoMapper);

        ReceiptDtoMapper receiptDtoMapper = new ReceiptDtoMapper();
        receiptApplicationService = new ReceiptApplicationService(orderRepository, paymentRepository, receiptDtoMapper);

        activityLogApplicationService = new ActivityLogApplicationService(activityLogRepository, objectMapper);
        auditEventListener = new AuditEventListener(activityLogApplicationService);

        reportingApplicationService = new ReportingApplicationService(orderRepository, paymentRepository, posSessionRepository, cafeRepository, exportJobRepository, exportJobRunner);
    }

    @Test
    @DisplayName("Complete First-Sale Vertical Slice End-to-End Workflow Verification")
    void verifyCompleteFirstSaleVerticalSliceSequence() {
        // =========================================================================
        // STEP 1: Admin onboard cafe & cashier (OnboardCafeUseCase)
        // =========================================================================
        OnboardCafeCommand onboardCmd = new OnboardCafeCommand(
                "Odoo Artisan Cafe",
                "odoo-artisan-cafe",
                "UTC",
                "USD",
                "Alice Owner",
                "admin@odoocafe.com",
                "AdminSecret123!"
        );
        OnboardCafeResponse onboardResponse = onboardCafeUseCase.execute(onboardCmd);

        assertThat(onboardResponse).isNotNull();
        UUID cafeId = onboardResponse.cafeId();
        UUID adminEmployeeId = onboardResponse.employeeId();
        assertThat(onboardResponse.adminRole()).isEqualTo(EmployeeRole.OWNER);

        // Register Cashier
        Employee cashierEmployee = new Employee(
                UUID.randomUUID(),
                cafeId,
                "Bob Cashier",
                "cashier@odoocafe.com",
                EmployeeRole.CASHIER
        );
        employeeRepository.save(cashierEmployee);

        String encodedCashierPass = passwordEncodingService.encode("CashierSecret123!");
        UserAccount cashierAccount = new UserAccount(
                UUID.randomUUID(),
                cashierEmployee.getId(),
                encodedCashierPass
        );
        userAccountRepository.save(cashierAccount);

        auditEventListener.handleUserOnboarded(new UserOnboardedEvent(
                cafeId, adminEmployeeId, "admin@odoocafe.com", cashierEmployee.getId(),
                "cashier@odoocafe.com", "CASHIER", "{\"role\":\"CASHIER\"}", "127.0.0.1", Instant.now()
        ));

        // =========================================================================
        // STEP 2: Admin configures category, product, tax, floor, table, and cash method
        // =========================================================================
        // 2a. Category
        CreateCategoryRequest categoryReq = new CreateCategoryRequest("Specialty Beverages", "#8B4513", 1);
        CategoryResponse categoryResp = categoryApplicationService.createCategory(cafeId, categoryReq);
        assertThat(categoryResp).isNotNull();
        assertThat(categoryResp.name()).isEqualTo("Specialty Beverages");

        // 2b. Product (priced at $6.50, kitchen routed)
        CreateProductRequest productReq = new CreateProductRequest(
                categoryResp.id(),
                "Flat White Coffee",
                "FW-1001",
                new BigDecimal("6.50"),
                "USD",
                UnitOfMeasure.UNIT,
                true
        );
        ProductResponse productResp = productApplicationService.createProduct(cafeId, productReq);
        assertThat(productResp).isNotNull();
        assertThat(productResp.name()).isEqualTo("Flat White Coffee");
        assertThat(productResp.priceAmount()).isEqualByComparingTo("6.50");
        assertThat(productResp.kitchenRouted()).isTrue();

        // 2c. Floor & Table
        Floor mainFloor = new Floor(cafeId, "Main Floor", 1);
        floorRepository.save(mainFloor);

        DiningTable diningTable = new DiningTable(cafeId, mainFloor.getId(), "T-01", 4);
        diningTableRepository.save(diningTable);

        // 2d. Terminal & Cash method
        Terminal posTerminal = new Terminal(cafeId, "Terminal 1", "TERM-01");
        terminalRepository.save(posTerminal);

        // =========================================================================
        // STEP 3: Cashier logs in (AuthenticateUserUseCase) and retrieves JWT
        // =========================================================================
        LoginCommand loginCmd = new LoginCommand(cafeId, "cashier@odoocafe.com", "CashierSecret123!");
        LoginResponse loginResp = authenticateUserUseCase.authenticate(loginCmd);

        assertThat(loginResp).isNotNull();
        assertThat(loginResp.accessToken()).isNotBlank();
        assertThat(loginResp.tokenType()).isEqualTo("Bearer");
        assertThat(loginResp.employeeId()).isEqualTo(cashierEmployee.getId());
        assertThat(loginResp.role()).isEqualTo(EmployeeRole.CASHIER);

        // =========================================================================
        // STEP 4: Cashier opens POS shift session (OpenPosSessionUseCase)
        // =========================================================================
        OpenSessionRequest openSessionReq = new OpenSessionRequest(posTerminal.getId(), new BigDecimal("100.00"));
        PosSessionResponse sessionResp = posSessionApplicationService.openSession(cafeId, cashierEmployee.getId(), openSessionReq);

        assertThat(sessionResp).isNotNull();
        assertThat(sessionResp.status()).isEqualTo(PosSessionStatus.OPEN);
        assertThat(sessionResp.openingBalanceAmount()).isEqualByComparingTo("100.00");

        auditEventListener.handlePosSessionOpened(new PosSessionOpenedEvent(
                cafeId, cashierEmployee.getId(), "cashier@odoocafe.com", sessionResp.id(),
                posTerminal.getId(), "{\"openingBalance\":100.00}", "127.0.0.1", Instant.now()
        ));

        // =========================================================================
        // STEP 5: Cashier selects dining table and creates draft order
        // =========================================================================
        CreateOrderRequest createOrderReq = new CreateOrderRequest(
                sessionResp.id(),
                OrderType.DINE_IN,
                diningTable.getId(),
                "Table 1 First Order",
                "USD"
        );
        OrderResponse orderResp = orderApplicationService.createOrder(cafeId, createOrderReq);

        assertThat(orderResp).isNotNull();
        assertThat(orderResp.status()).isEqualTo(OrderStatus.DRAFT);
        assertThat(orderResp.diningTableId()).isEqualTo(diningTable.getId());
        assertThat(orderResp.orderNumber()).startsWith("ORD-");

        // =========================================================================
        // STEP 6: Cashier adds product lines (pricing engine calculates subtotals)
        // =========================================================================
        // Add 2 Flat White Coffees @ $6.50 = $13.00
        AddOrderLineRequest addLineReq = new AddOrderLineRequest(productResp.id(), 2);
        OrderResponse updatedOrderResp = orderApplicationService.addOrderLine(cafeId, orderResp.id(), addLineReq);

        assertThat(updatedOrderResp.lines()).hasSize(1);
        assertThat(updatedOrderResp.totalAmount()).isEqualByComparingTo("13.00");

        // =========================================================================
        // STEP 7: Order items are dispatched to kitchen idempotently (KitchenDispatchService)
        // =========================================================================
        DispatchOrderToKitchenResponse dispatchResp = kitchenApplicationService.dispatchOrderToKitchen(cafeId, updatedOrderResp.id());

        assertThat(dispatchResp).isNotNull();
        assertThat(dispatchResp.ticketId()).isNotNull();
        assertThat(dispatchResp.items()).hasSize(1);

        // Verify KitchenDispatchService idempotency
        Order currentOrder = orderRepository.findById(updatedOrderResp.id()).orElseThrow();
        KitchenTicket ticketAttempt1 = kitchenDispatchService.dispatchOrder(currentOrder);
        KitchenTicket ticketAttempt2 = kitchenDispatchService.dispatchOrder(currentOrder);

        assertThat(ticketAttempt1.getId()).isEqualTo(ticketAttempt2.getId());
        assertThat(ticketAttempt2.getItems()).hasSize(1);

        // =========================================================================
        // STEP 8: Outbox publisher processes kitchen dispatch event (OutboxPublisher)
        // =========================================================================
        assertThat(outboxEventRepository.findByStatus(OutboxEventStatus.PENDING)).isNotEmpty();
        outboxPublisher.publishPendingEvents();

        List<OutboxEvent> processedOutboxEvents = outboxEventRepository.findByStatus(OutboxEventStatus.PROCESSED);
        assertThat(processedOutboxEvents).isNotEmpty();
        assertThat(processedOutboxEvents.get(0).getEventType()).isEqualTo("KitchenTicketDispatchedEvent");

        auditEventListener.handleKitchenTicketDispatched(new KitchenTicketDispatchedEvent(
                dispatchResp.ticketId(), cafeId, updatedOrderResp.id(), updatedOrderResp.orderNumber(),
                List.of(), Instant.now()
        ));

        // =========================================================================
        // STEP 9: Kitchen completes ticket items and ticket (KitchenApplicationService)
        // =========================================================================
        KitchenTicketResponse ticketBeforeCompletion = kitchenApplicationService.getKitchenTicketById(cafeId, dispatchResp.ticketId());
        assertThat(ticketBeforeCompletion.status()).isNotEqualTo(KitchenTicketStatus.COMPLETED);

        KitchenTicketResponse completedTicketResp = kitchenApplicationService.completeTicket(cafeId, dispatchResp.ticketId());
        assertThat(completedTicketResp.status()).isEqualTo(KitchenTicketStatus.COMPLETED);

        for (var item : completedTicketResp.items()) {
            assertThat(item.status()).isEqualTo(KitchenItemStatus.COMPLETED);
        }

        // =========================================================================
        // STEP 10: Cashier records cash payment and receives calculated change (PaymentApplicationService)
        // =========================================================================
        // Tender $20.00 cash for $13.00 total order -> $7.00 change expected
        ProcessCashPaymentRequest cashPaymentReq = new ProcessCashPaymentRequest(new BigDecimal("20.00"));
        PaymentResponse paymentResp = paymentApplicationService.processCashPayment(cafeId, updatedOrderResp.id(), cashPaymentReq);

        assertThat(paymentResp).isNotNull();
        assertThat(paymentResp.paymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(paymentResp.amount()).isEqualByComparingTo("13.00");
        assertThat(paymentResp.tenderedAmount()).isEqualByComparingTo("20.00");
        assertThat(paymentResp.changeAmount()).isEqualByComparingTo("7.00");
        assertThat(paymentResp.status()).isEqualTo(PaymentStatus.COMPLETED);

        // Record cash sale movement in shift session ($13.00 cash in)
        posSessionApplicationService.recordCashMovement(
                cafeId,
                sessionResp.id(),
                cashierEmployee.getId(),
                new RecordCashMovementRequest(CashMovementType.CASH_IN, new BigDecimal("13.00"), "Cash sale order " + updatedOrderResp.orderNumber())
        );

        auditEventListener.handlePaymentProcessed(new PaymentProcessedEvent(
                cafeId, cashierEmployee.getId(), "cashier@odoocafe.com", paymentResp.id(),
                updatedOrderResp.id(), "CASH", "{\"amount\":13.00,\"paymentMethod\":\"CASH\"}", "127.0.0.1", Instant.now()
        ));

        // =========================================================================
        // STEP 11: Order status transitions to PAID (paid order immutability verified)
        // =========================================================================
        OrderResponse paidOrderResp = orderApplicationService.getOrderById(cafeId, updatedOrderResp.id());
        assertThat(paidOrderResp.status()).isEqualTo(OrderStatus.PAID);

        // Verify paid order immutability: modification attempt throws BusinessException
        assertThatThrownBy(() -> orderApplicationService.addOrderLine(cafeId, paidOrderResp.id(), new AddOrderLineRequest(productResp.id(), 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Order lines can only be modified when order is in DRAFT or CONFIRMED state");

        // Verify paid order immutability: cancellation attempt throws BusinessException
        assertThatThrownBy(() -> orderApplicationService.cancelOrder(cafeId, paidOrderResp.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot cancel order in status: PAID");

        // =========================================================================
        // STEP 12: Receipt payload is generated (ReceiptApplicationService)
        // =========================================================================
        ReceiptResponse receiptResp = receiptApplicationService.getReceiptForOrder(cafeId, paidOrderResp.id());

        assertThat(receiptResp).isNotNull();
        assertThat(receiptResp.orderId()).isEqualTo(paidOrderResp.id());
        assertThat(receiptResp.orderNumber()).isEqualTo(paidOrderResp.orderNumber());
        assertThat(receiptResp.totalAmount()).isEqualByComparingTo("13.00");
        assertThat(receiptResp.payment().tenderedAmount()).isEqualByComparingTo("20.00");
        assertThat(receiptResp.payment().changeAmount()).isEqualByComparingTo("7.00");
        assertThat(receiptResp.payment().paymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(receiptResp.lines()).hasSize(1);

        // =========================================================================
        // STEP 13: Cashier closes and reconciles session shift with expected balance and zero cash variance (ClosePosSessionUseCase)
        // =========================================================================
        // Opening balance $100.00 + Cash In $13.00 = Expected balance $113.00. Counted $113.00 -> Variance 0.00
        CloseSessionRequest closeReq = new CloseSessionRequest(new BigDecimal("113.00"));
        PosSessionResponse closedSessionResp = posSessionApplicationService.closeSession(
                cafeId,
                sessionResp.id(),
                cashierEmployee.getId(),
                closeReq
        );

        assertThat(closedSessionResp).isNotNull();
        assertThat(closedSessionResp.status()).isEqualTo(PosSessionStatus.CLOSED);
        assertThat(closedSessionResp.closingExpectedAmount()).isEqualByComparingTo("113.00");
        assertThat(closedSessionResp.closingCountedAmount()).isEqualByComparingTo("113.00");
        assertThat(closedSessionResp.closingVarianceAmount()).isEqualByComparingTo("0.00");

        auditEventListener.handlePosSessionClosed(new PosSessionClosedEvent(
                cafeId, cashierEmployee.getId(), "cashier@odoocafe.com", sessionResp.id(),
                "{\"closingExpectedAmount\":113.00,\"closingCountedAmount\":113.00,\"variance\":0.00}", "127.0.0.1", Instant.now()
        ));

        // =========================================================================
        // STEP 14: Activity logs & reports verify full workflow trace
        // =========================================================================
        Page<ActivityLog> logPage = activityLogApplicationService.searchActivityLogs(cafeId, Pageable.unpaged());
        assertThat(logPage.getContent()).isNotEmpty();
        assertThat(logPage.getContent().stream().map(ActivityLog::getAction)).contains(
                "USER_ONBOARDED",
                "SESSION_OPENED",
                "KITCHEN_DISPATCH",
                "PAYMENT_PROCESSED",
                "SESSION_CLOSED"
        );

        SalesSummaryProjection summary = reportingApplicationService.getSalesSummary(cafeId, null, null);
        assertThat(summary).isNotNull();
        assertThat(summary.totalOrders()).isEqualTo(1L);
        assertThat(summary.netRevenue()).isEqualByComparingTo("13.00");
        assertThat(summary.cashVariance()).isEqualByComparingTo("0.00");
        assertThat(summary.paymentMethodBreakdown().get("CASH")).isEqualByComparingTo("13.00");
    }
}
