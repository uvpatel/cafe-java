package com.cafepos.shared.seed;

import com.cafepos.catalog.domain.ProductCategory;
import com.cafepos.catalog.domain.ProductCategoryRepository;
import com.cafepos.catalog.domain.ProductRepository;
import com.cafepos.floor.domain.Floor;
import com.cafepos.floor.domain.FloorRepository;
import com.cafepos.floor.domain.DiningTableRepository;
import com.cafepos.identity.domain.Employee;
import com.cafepos.identity.domain.EmployeeRepository;
import com.cafepos.identity.domain.UserAccountRepository;
import com.cafepos.identity.infrastructure.PasswordEncodingService;
import com.cafepos.organization.domain.Cafe;
import com.cafepos.organization.domain.CafeRepository;
import com.cafepos.promotion.domain.CouponRepository;
import com.cafepos.session.domain.TerminalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseSeederTest {

    @Mock
    private CafeRepository cafeRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private ProductCategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private FloorRepository floorRepository;

    @Mock
    private DiningTableRepository diningTableRepository;

    @Mock
    private TerminalRepository terminalRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private PasswordEncodingService passwordEncodingService;

    @InjectMocks
    private DatabaseSeeder seeder;

    @Test
    @DisplayName("DatabaseSeeder should seed entities idempotently when natural keys are missing")
    void testSeederExecutionIdempotent() {
        UUID cafeId = UUID.randomUUID();
        Cafe cafe = new Cafe(cafeId, "Central Perk Cafe", "central-perk", "UTC", "USD");
        ProductCategory category = new ProductCategory(UUID.randomUUID(), cafeId, "Hot Specialty Coffees", "#8B4513", 1);
        Floor floor = new Floor(UUID.randomUUID(), cafeId, "Main Dining Room", 1);

        when(cafeRepository.findBySlug("central-perk")).thenReturn(Optional.of(cafe));
        when(employeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(categoryRepository.existsByCafeIdAndNameIgnoreCase(eq(cafeId), any())).thenReturn(false);
        when(categoryRepository.save(any())).thenReturn(category);
        when(floorRepository.existsByCafeIdAndNameIgnoreCase(eq(cafeId), any())).thenReturn(false);
        when(floorRepository.save(any())).thenReturn(floor);
        when(passwordEncodingService.encode(any())).thenReturn("encodedPassword");

        // Run seeder once
        seeder.run();

        verify(employeeRepository, times(5)).save(any());
        verify(productRepository, times(5)).save(any());
        verify(terminalRepository, times(2)).save(any());
        verify(couponRepository, times(2)).save(any());

        // Configure mocks for second run where natural keys exist
        when(employeeRepository.findByCafeIdAndEmail(eq(cafeId), any())).thenReturn(Optional.of(mock(Employee.class)));
        when(productRepository.existsByCafeIdAndSku(eq(cafeId), any())).thenReturn(true);
        when(terminalRepository.existsByCafeIdAndCodeIgnoreCase(eq(cafeId), any())).thenReturn(true);
        when(couponRepository.existsByCafeIdAndCodeIgnoreCase(eq(cafeId), any())).thenReturn(true);

        // Run seeder second time
        seeder.run();

        // Verify productRepository.save was NOT called additional times on second run
        verify(productRepository, times(5)).save(any());
    }
}
