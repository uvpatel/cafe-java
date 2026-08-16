package com.cafepos.shared.seed;

import com.cafepos.catalog.domain.Product;
import com.cafepos.catalog.domain.ProductCategory;
import com.cafepos.catalog.domain.ProductCategoryRepository;
import com.cafepos.catalog.domain.ProductRepository;
import com.cafepos.catalog.domain.UnitOfMeasure;
import com.cafepos.floor.domain.DiningTable;
import com.cafepos.floor.domain.DiningTableRepository;
import com.cafepos.floor.domain.Floor;
import com.cafepos.floor.domain.FloorRepository;
import com.cafepos.identity.domain.Employee;
import com.cafepos.identity.domain.EmployeeRepository;
import com.cafepos.identity.domain.EmployeeRole;
import com.cafepos.identity.domain.UserAccount;
import com.cafepos.identity.domain.UserAccountRepository;
import com.cafepos.identity.infrastructure.PasswordEncodingService;
import com.cafepos.organization.domain.Cafe;
import com.cafepos.organization.domain.CafeRepository;
import com.cafepos.promotion.domain.Coupon;
import com.cafepos.promotion.domain.CouponRepository;
import com.cafepos.promotion.domain.DiscountType;
import com.cafepos.session.domain.Terminal;
import com.cafepos.session.domain.TerminalRepository;
import com.cafepos.shared.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final CafeRepository cafeRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final FloorRepository floorRepository;
    private final DiningTableRepository diningTableRepository;
    private final TerminalRepository terminalRepository;
    private final CouponRepository couponRepository;
    private final PasswordEncodingService passwordEncodingService;

    @Value("${app.seed.admin-password:password123}")
    private String demoPassword;

    public DatabaseSeeder(CafeRepository cafeRepository,
                          EmployeeRepository employeeRepository,
                          UserAccountRepository userAccountRepository,
                          ProductCategoryRepository categoryRepository,
                          ProductRepository productRepository,
                          FloorRepository floorRepository,
                          DiningTableRepository diningTableRepository,
                          TerminalRepository terminalRepository,
                          CouponRepository couponRepository,
                          PasswordEncodingService passwordEncodingService) {
        this.cafeRepository = Objects.requireNonNull(cafeRepository);
        this.employeeRepository = Objects.requireNonNull(employeeRepository);
        this.userAccountRepository = Objects.requireNonNull(userAccountRepository);
        this.categoryRepository = Objects.requireNonNull(categoryRepository);
        this.productRepository = Objects.requireNonNull(productRepository);
        this.floorRepository = Objects.requireNonNull(floorRepository);
        this.diningTableRepository = Objects.requireNonNull(diningTableRepository);
        this.terminalRepository = Objects.requireNonNull(terminalRepository);
        this.couponRepository = Objects.requireNonNull(couponRepository);
        this.passwordEncodingService = Objects.requireNonNull(passwordEncodingService);
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedAll();
    }

    @Transactional
    public void seedAll() {
        log.info("Starting safe, idempotent database data seeding...");

        // 1. Seed Cafe Organization
        String cafeSlug = "central-perk";
        Cafe cafe = cafeRepository.findBySlug(cafeSlug)
                .orElseGet(() -> cafeRepository.save(new Cafe(null, "Central Perk Cafe", cafeSlug, "UTC", "USD")));
        UUID cafeId = cafe.getId();

        // 2. Seed Employees & User Accounts
        seedEmployeeAndUser(cafeId, "Rachel Green", "rachel@centralperk.com", EmployeeRole.OWNER);
        seedEmployeeAndUser(cafeId, "Monica Geller", "monica@centralperk.com", EmployeeRole.MANAGER);
        seedEmployeeAndUser(cafeId, "Chandler Bing", "chandler@centralperk.com", EmployeeRole.CASHIER);
        seedEmployeeAndUser(cafeId, "Joey Tribbiani", "joey@centralperk.com", EmployeeRole.KITCHEN_STAFF);
        seedEmployeeAndUser(cafeId, "Phoebe Buffay", "phoebe@centralperk.com", EmployeeRole.CASHIER);

        // 3. Seed Catalog Categories
        ProductCategory hotCoffee = seedCategory(cafeId, "Hot Specialty Coffees", "#8B4513", 1);
        ProductCategory bakery = seedCategory(cafeId, "Fresh Pastries & Bakery", "#D2691E", 2);
        ProductCategory coldDrinks = seedCategory(cafeId, "Iced Drinks & Beverages", "#008080", 3);

        // 4. Seed Products
        seedProduct(cafeId, hotCoffee.getId(), "Artisan Caramel Macchiato", "COF-CAR-001", new BigDecimal("5.50"), true);
        seedProduct(cafeId, hotCoffee.getId(), "Classic Cappuccino", "COF-CAP-002", new BigDecimal("4.75"), true);
        seedProduct(cafeId, bakery.getId(), "French Butter Croissant", "BAK-CRO-001", new BigDecimal("3.75"), false);
        seedProduct(cafeId, bakery.getId(), "Chocolate Chip Cookie", "BAK-COO-002", new BigDecimal("2.50"), false);
        seedProduct(cafeId, coldDrinks.getId(), "Iced Matcha Latte", "DRK-MAT-001", new BigDecimal("5.25"), true);

        // 5. Seed POS Terminals
        seedTerminal(cafeId, "Main Counter Terminal", "TERM-01");
        seedTerminal(cafeId, "Express Patio Terminal", "TERM-02");

        // 6. Seed Floor & Tables
        Floor mainFloor = seedFloor(cafeId, "Main Dining Room", 1);
        seedTable(cafeId, mainFloor.getId(), "T-01", 4);
        seedTable(cafeId, mainFloor.getId(), "T-02", 2);
        seedTable(cafeId, mainFloor.getId(), "T-03", 6);

        // 7. Seed Coupons
        seedCoupon(cafeId, "WELCOME10", DiscountType.PERCENTAGE, new BigDecimal("10.00"));
        seedCoupon(cafeId, "SUMMER20", DiscountType.PERCENTAGE, new BigDecimal("20.00"));

        log.info("Database data seeding completed successfully. Summary: Cafes={}, Employees={}, Categories={}, Products={}, Terminals={}",
                cafeRepository.count(), employeeRepository.count(), categoryRepository.count(), productRepository.count(), terminalRepository.count());
    }

    private void seedEmployeeAndUser(UUID cafeId, String name, String email, EmployeeRole role) {
        Optional<Employee> existing = employeeRepository.findByCafeIdAndEmail(cafeId, email);
        if (existing.isEmpty()) {
            Employee emp = employeeRepository.save(new Employee(null, cafeId, name, email, role));
            String encodedPassword = passwordEncodingService.encode(demoPassword);

            userAccountRepository.save(new UserAccount(emp.getId(), encodedPassword));
        }
    }

    private ProductCategory seedCategory(UUID cafeId, String name, String colorHex, int sortOrder) {
        if (!categoryRepository.existsByCafeIdAndNameIgnoreCase(cafeId, name)) {
            return categoryRepository.save(new ProductCategory(cafeId, name, colorHex, sortOrder));
        }
        return categoryRepository.findByCafeId(cafeId).stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> categoryRepository.save(new ProductCategory(cafeId, name, colorHex, sortOrder)));
    }

    private void seedProduct(UUID cafeId, UUID categoryId, String name, String sku, BigDecimal price, boolean kitchenRouted) {
        if (!productRepository.existsByCafeIdAndSku(cafeId, sku)) {
            productRepository.save(new Product(null, cafeId, categoryId, name, sku, Money.of(price, "USD"), UnitOfMeasure.PIECE, kitchenRouted));
        }
    }

    private void seedTerminal(UUID cafeId, String name, String code) {
        if (!terminalRepository.existsByCafeIdAndCodeIgnoreCase(cafeId, code)) {
            terminalRepository.save(new Terminal(cafeId, name, code));
        }
    }

    private Floor seedFloor(UUID cafeId, String name, int sortOrder) {
        if (!floorRepository.existsByCafeIdAndNameIgnoreCase(cafeId, name)) {
            return floorRepository.save(new Floor(cafeId, name, sortOrder));
        }
        return floorRepository.findByCafeId(cafeId).stream()
                .filter(f -> f.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> floorRepository.save(new Floor(cafeId, name, sortOrder)));
    }

    private void seedTable(UUID cafeId, UUID floorId, String tableNumber, int capacity) {
        if (!diningTableRepository.existsByCafeIdAndFloorIdAndTableNumberIgnoreCase(cafeId, floorId, tableNumber)) {
            diningTableRepository.save(new DiningTable(cafeId, floorId, tableNumber, capacity));
        }
    }

    private void seedCoupon(UUID cafeId, String code, DiscountType type, BigDecimal value) {
        if (!couponRepository.existsByCafeIdAndCodeIgnoreCase(cafeId, code)) {
            couponRepository.save(new Coupon(cafeId, code, type, value));
        }
    }
}
