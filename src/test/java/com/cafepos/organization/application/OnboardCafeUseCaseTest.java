package com.cafepos.organization.application;

import com.cafepos.identity.domain.Employee;
import com.cafepos.identity.domain.EmployeeRepository;
import com.cafepos.identity.domain.EmployeeRole;
import com.cafepos.identity.domain.UserAccount;
import com.cafepos.identity.domain.UserAccountRepository;
import com.cafepos.identity.infrastructure.PasswordEncodingService;
import com.cafepos.organization.domain.Cafe;
import com.cafepos.organization.domain.CafeRepository;
import com.cafepos.shared.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardCafeUseCaseTest {

    @Mock
    private CafeRepository cafeRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncodingService passwordEncodingService;

    @InjectMocks
    private OnboardCafeUseCase onboardCafeUseCase;

    private OnboardCafeCommand validCommand;

    @BeforeEach
    void setUp() {
        validCommand = new OnboardCafeCommand(
                "Artisan Coffee",
                "artisan-coffee",
                "America/New_York",
                "USD",
                "Jane Doe",
                "JANE@ARTISANCOFFEE.COM ",
                "SecurePass123!"
        );
    }

    @Test
    @DisplayName("Should successfully onboard a new cafe with initial OWNER employee and encoded password")
    void shouldOnboardCafeSuccessfully() {
        // Arrange
        when(cafeRepository.existsBySlug("artisan-coffee")).thenReturn(false);
        when(passwordEncodingService.encode("SecurePass123!")).thenReturn("encoded_SecurePass123!");

        when(cafeRepository.save(any(Cafe.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OnboardCafeResponse response = onboardCafeUseCase.execute(validCommand);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.cafeId()).isNotNull();
        assertThat(response.employeeId()).isNotNull();
        assertThat(response.adminRole()).isEqualTo(EmployeeRole.OWNER);

        // Verify captured Cafe entity
        ArgumentCaptor<Cafe> cafeCaptor = ArgumentCaptor.forClass(Cafe.class);
        verify(cafeRepository).save(cafeCaptor.capture());
        Cafe savedCafe = cafeCaptor.getValue();
        assertThat(savedCafe.getName()).isEqualTo("Artisan Coffee");
        assertThat(savedCafe.getSlug()).isEqualTo("artisan-coffee");
        assertThat(savedCafe.getTimezone()).isEqualTo("America/New_York");
        assertThat(savedCafe.getCurrency()).isEqualTo("USD");

        // Verify captured Employee entity (with normalized email and forced OWNER role)
        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(employeeCaptor.capture());
        Employee savedEmployee = employeeCaptor.getValue();
        assertThat(savedEmployee.getCafeId()).isEqualTo(savedCafe.getId());
        assertThat(savedEmployee.getFullName()).isEqualTo("Jane Doe");
        assertThat(savedEmployee.getEmail()).isEqualTo("jane@artisancoffee.com");
        assertThat(savedEmployee.getRole()).isEqualTo(EmployeeRole.OWNER);

        // Verify captured UserAccount entity
        ArgumentCaptor<UserAccount> userAccountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(userAccountCaptor.capture());
        UserAccount savedUserAccount = userAccountCaptor.getValue();
        assertThat(savedUserAccount.getEmployeeId()).isEqualTo(savedEmployee.getId());
        assertThat(savedUserAccount.getPasswordHash()).isEqualTo("encoded_SecurePass123!");
    }

    @Test
    @DisplayName("Should reject onboarding when cafe slug is already taken")
    void shouldRejectDuplicateSlug() {
        // Arrange
        when(cafeRepository.existsBySlug("artisan-coffee")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> onboardCafeUseCase.execute(validCommand))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("artisan-coffee")
                .hasFieldOrPropertyWithValue("code", "DUPLICATE_SLUG")
                .hasFieldOrPropertyWithValue("status", 400);

        verify(cafeRepository, never()).save(any());
        verify(employeeRepository, never()).save(any());
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should enforce OWNER role regardless of any command inputs or external parameters")
    void shouldAlwaysAssignOwnerRoleToAdmin() {
        // Arrange
        OnboardCafeCommand command = new OnboardCafeCommand(
                "My Cafe",
                "MY-CAFE ",
                "UTC",
                "EUR",
                "Admin User",
                "admin@mycafe.com",
                "Password123"
        );
        when(cafeRepository.existsBySlug("my-cafe")).thenReturn(false);
        when(passwordEncodingService.encode("Password123")).thenReturn("encoded_Password123");
        when(cafeRepository.save(any(Cafe.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OnboardCafeResponse response = onboardCafeUseCase.execute(command);

        // Assert
        assertThat(response.adminRole()).isEqualTo(EmployeeRole.OWNER);

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(employeeCaptor.capture());
        assertThat(employeeCaptor.getValue().getRole()).isEqualTo(EmployeeRole.OWNER);
    }
}
