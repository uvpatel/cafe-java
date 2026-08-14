package com.cafepos.identity.application;

import com.cafepos.identity.domain.Employee;
import com.cafepos.identity.domain.EmployeeRepository;
import com.cafepos.identity.domain.EmployeeRole;
import com.cafepos.identity.domain.UserAccount;
import com.cafepos.identity.domain.UserAccountRepository;
import com.cafepos.identity.infrastructure.JwtTokenClaims;
import com.cafepos.identity.infrastructure.JwtTokenService;
import com.cafepos.identity.infrastructure.PasswordEncodingService;
import com.cafepos.shared.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserUseCaseTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncodingService passwordEncodingService;

    @Mock
    private JwtTokenService jwtTokenService;

    private AuthenticateUserUseCase authenticateUserUseCase;

    private UUID cafeId;
    private UUID employeeId;
    private Employee employee;
    private UserAccount userAccount;

    @BeforeEach
    void setUp() {
        authenticateUserUseCase = new AuthenticateUserUseCase(
                employeeRepository,
                userAccountRepository,
                passwordEncodingService,
                jwtTokenService
        );

        cafeId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        employee = new Employee(employeeId, cafeId, "John Doe", "john@cafepos.com", EmployeeRole.CASHIER);
        userAccount = new UserAccount(employeeId, "$2a$10$encodedPasswordHash");
    }

    @Test
    @DisplayName("Should authenticate user successfully with valid credentials")
    void shouldAuthenticateSuccessfully() {
        LoginCommand command = new LoginCommand(cafeId, "john@cafepos.com", "correctPassword");

        when(employeeRepository.findByCafeIdAndEmail(cafeId, "john@cafepos.com"))
                .thenReturn(Optional.of(employee));
        when(userAccountRepository.findByEmployeeId(employeeId))
                .thenReturn(Optional.of(userAccount));
        when(passwordEncodingService.matches("correctPassword", userAccount.getPasswordHash()))
                .thenReturn(true);
        when(jwtTokenService.generateAccessToken(cafeId, employeeId, "john@cafepos.com", "CASHIER"))
                .thenReturn("access-token-123");
        when(jwtTokenService.generateRefreshToken(cafeId, employeeId))
                .thenReturn("refresh-token-123");
        when(jwtTokenService.getAccessTokenExpirationSeconds())
                .thenReturn(900L);

        LoginResponse response = authenticateUserUseCase.authenticate(command);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token-123");
        assertThat(response.refreshToken()).isEqualTo("refresh-token-123");
        assertThat(response.employeeId()).isEqualTo(employeeId);
        assertThat(response.cafeId()).isEqualTo(cafeId);
        assertThat(response.role()).isEqualTo(EmployeeRole.CASHIER);

        ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getFailedAttempts()).isEqualTo(0);
        assertThat(accountCaptor.getValue().getLastLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("Should increment failed attempts on wrong password")
    void shouldFailWhenPasswordIsIncorrect() {
        LoginCommand command = new LoginCommand(cafeId, "john@cafepos.com", "wrongPassword");

        when(employeeRepository.findByCafeIdAndEmail(cafeId, "john@cafepos.com"))
                .thenReturn(Optional.of(employee));
        when(userAccountRepository.findByEmployeeId(employeeId))
                .thenReturn(Optional.of(userAccount));
        when(passwordEncodingService.matches("wrongPassword", userAccount.getPasswordHash()))
                .thenReturn(false);

        assertThatThrownBy(() -> authenticateUserUseCase.authenticate(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid email or password");

        ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getFailedAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should lock account after 5 consecutive failed attempts")
    void shouldLockAccountAfterFiveConsecutiveFailedAttempts() {
        LoginCommand command = new LoginCommand(cafeId, "john@cafepos.com", "wrongPassword");

        // Simulate 4 previous failed attempts
        for (int i = 0; i < 4; i++) {
            userAccount.recordFailedAttempt(Instant.now());
        }

        when(employeeRepository.findByCafeIdAndEmail(cafeId, "john@cafepos.com"))
                .thenReturn(Optional.of(employee));
        when(userAccountRepository.findByEmployeeId(employeeId))
                .thenReturn(Optional.of(userAccount));
        when(passwordEncodingService.matches("wrongPassword", userAccount.getPasswordHash()))
                .thenReturn(false);

        assertThatThrownBy(() -> authenticateUserUseCase.authenticate(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("locked");

        assertThat(userAccount.getFailedAttempts()).isEqualTo(5);
        assertThat(userAccount.getLockedUntil()).isNotNull();
    }

    @Test
    @DisplayName("Should reject login for already locked account")
    void shouldRejectLoginForLockedAccount() {
        for (int i = 0; i < 5; i++) {
            userAccount.recordFailedAttempt(Instant.now());
        }

        LoginCommand command = new LoginCommand(cafeId, "john@cafepos.com", "anyPassword");

        when(employeeRepository.findByCafeIdAndEmail(cafeId, "john@cafepos.com"))
                .thenReturn(Optional.of(employee));
        when(userAccountRepository.findByEmployeeId(employeeId))
                .thenReturn(Optional.of(userAccount));

        assertThatThrownBy(() -> authenticateUserUseCase.authenticate(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("locked");
    }

    @Test
    @DisplayName("Should reject login for inactive employee")
    void shouldRejectLoginForInactiveEmployee() {
        employee.setActive(false);
        LoginCommand command = new LoginCommand(cafeId, "john@cafepos.com", "correctPassword");

        when(employeeRepository.findByCafeIdAndEmail(cafeId, "john@cafepos.com"))
                .thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> authenticateUserUseCase.authenticate(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    @DisplayName("Should reset failed attempts on successful login")
    void shouldResetFailedAttemptsOnSuccessfulLogin() {
        userAccount.recordFailedAttempt(Instant.now());
        userAccount.recordFailedAttempt(Instant.now());
        assertThat(userAccount.getFailedAttempts()).isEqualTo(2);

        LoginCommand command = new LoginCommand(cafeId, "john@cafepos.com", "correctPassword");

        when(employeeRepository.findByCafeIdAndEmail(cafeId, "john@cafepos.com"))
                .thenReturn(Optional.of(employee));
        when(userAccountRepository.findByEmployeeId(employeeId))
                .thenReturn(Optional.of(userAccount));
        when(passwordEncodingService.matches("correctPassword", userAccount.getPasswordHash()))
                .thenReturn(true);
        when(jwtTokenService.generateAccessToken(any(), any(), any(), any()))
                .thenReturn("access-token-123");
        when(jwtTokenService.generateRefreshToken(any(), any()))
                .thenReturn("refresh-token-123");

        authenticateUserUseCase.authenticate(command);

        assertThat(userAccount.getFailedAttempts()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void shouldRefreshTokenSuccessfully() {
        RefreshTokenCommand command = new RefreshTokenCommand("valid-refresh-token");
        JwtTokenClaims claims = new JwtTokenClaims(cafeId, employeeId, "john@cafepos.com", "CASHIER", "REFRESH");

        when(jwtTokenService.parseToken("valid-refresh-token")).thenReturn(claims);
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(userAccountRepository.findByEmployeeId(employeeId)).thenReturn(Optional.of(userAccount));
        when(jwtTokenService.generateAccessToken(cafeId, employeeId, "john@cafepos.com", "CASHIER"))
                .thenReturn("new-access-token");
        when(jwtTokenService.generateRefreshToken(cafeId, employeeId))
                .thenReturn("new-refresh-token");
        when(jwtTokenService.getAccessTokenExpirationSeconds()).thenReturn(900L);

        RefreshTokenResponse response = authenticateUserUseCase.refreshToken(command);

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("Should reject refresh when token type is not REFRESH")
    void shouldRejectRefreshWithAccessToken() {
        RefreshTokenCommand command = new RefreshTokenCommand("access-token-used-as-refresh");
        JwtTokenClaims claims = new JwtTokenClaims(cafeId, employeeId, "john@cafepos.com", "CASHIER", "ACCESS");

        when(jwtTokenService.parseToken("access-token-used-as-refresh")).thenReturn(claims);

        assertThatThrownBy(() -> authenticateUserUseCase.refreshToken(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not a refresh token");
    }
}
