package com.cafepos.fixture;

import com.cafepos.catalog.domain.Product;
import com.cafepos.catalog.domain.ProductCategory;
import com.cafepos.catalog.domain.UnitOfMeasure;
import com.cafepos.identity.domain.Employee;
import com.cafepos.identity.domain.EmployeeRole;
import com.cafepos.identity.domain.UserAccount;
import com.cafepos.organization.domain.Cafe;
import com.cafepos.session.domain.Terminal;
import com.cafepos.shared.money.Money;

import java.math.BigDecimal;
import java.util.UUID;

public class CafeFixture {

    public static Cafe createCafe() {
        return new Cafe(null, "Test Cafe", "test-cafe", "UTC", "USD");
    }

    public static Employee createEmployee(UUID cafeId, EmployeeRole role) {
        return new Employee(null, cafeId, "John Doe", "john." + UUID.randomUUID().toString().substring(0, 8) + "@test.com", role);
    }

    public static UserAccount createUserAccount(UUID employeeId, String encodedPassword) {
        return new UserAccount(employeeId, encodedPassword);
    }

    public static ProductCategory createCategory(UUID cafeId) {
        return new ProductCategory(cafeId, "Hot Beverages", "#8B4513", 1);
    }

    public static Product createProduct(UUID cafeId, UUID categoryId) {
        return new Product(null, cafeId, categoryId, "Espresso", "SKU-" + UUID.randomUUID().toString().substring(0, 6), Money.of(new BigDecimal("3.50"), "USD"), UnitOfMeasure.PIECE, true);
    }

    public static Terminal createTerminal(UUID cafeId) {
        return new Terminal(cafeId, "Main Counter", "TERM-" + UUID.randomUUID().toString().substring(0, 4));
    }
}
