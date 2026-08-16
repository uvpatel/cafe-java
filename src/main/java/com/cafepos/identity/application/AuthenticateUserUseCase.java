package com.cafepos.identity.application;

import com.cafepos.identity.domain.Employee;
import com.cafepos.identity.domain.EmployeeRepository;
import com.cafepos.identity.domain.UserAccount;
import com.cafepos.identity.domain.UserAccountRepository;
import com.cafepos.identity.infrastructure.JwtTokenClaims;
import com.cafepos.identity.infrastructure.JwtTokenService;
import com.cafepos.identity.infrastructure.PasswordEncodingService;
import com.cafepos.shared.error.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class AuthenticateUserUseCase {

    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncodingService passwordEncodingService;
    private final JwtTokenService jwtTokenService;

    public AuthenticateUserUseCase(
            EmployeeRepository employeeRepository,
            UserAccountRepository userAccountRepository,
            PasswordEncodingService passwordEncodingService,
            JwtTokenService jwtTokenService) {
        this.employeeRepository = Objects.requireNonNull(employeeRepository, "employeeRepository must not be null");
        this.userAccountRepository = Objects.requireNonNull(userAccountRepository, "userAccountRepository must not be null");
        this.passwordEncodingService = Objects.requireNonNull(passwordEncodingService, "passwordEncodingService must not be null");
        this.jwtTokenService = Objects.requireNonNull(jwtTokenService, "jwtTokenService must not be null");
    }

    @Transactional
    public LoginResponse authenticate(LoginCommand command) {
        Objects.requireNonNull(command, "LoginCommand must not be null");

        String email = command.email() != null ? command.email().trim().toLowerCase() : "";

        Employee employee;
        if (command.cafeId() != null) {
            employee = employeeRepository.findByCafeIdAndEmail(command.cafeId(), email)
                    .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS", "Invalid email or password", 401));
        } else {
            employee = employeeRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS", "Invalid email or password", 401));
        }

        if (!employee.isActive()) {
            throw new BusinessException("EMPLOYEE_INACTIVE", "Employee account is inactive", 403);
        }

        UserAccount userAccount = userAccountRepository.findByEmployeeId(employee.getId())
                .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS", "Invalid email or password", 401));

        Instant now = Instant.now();

        if (userAccount.isLocked(now)) {
            throw new BusinessException("ACCOUNT_LOCKED", "Account is locked due to too many failed attempts", 423);
        }

        if (!passwordEncodingService.matches(command.password(), userAccount.getPasswordHash())) {
            userAccount.recordFailedAttempt(now);
            userAccountRepository.save(userAccount);

            if (userAccount.isLocked(now)) {
                throw new BusinessException("ACCOUNT_LOCKED", "Account is locked due to too many failed attempts", 423);
            }
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid email or password", 401);
        }

        userAccount.recordSuccessfulLogin(now);
        userAccountRepository.save(userAccount);

        String accessToken = jwtTokenService.generateAccessToken(
                employee.getCafeId(),
                employee.getId(),
                employee.getEmail(),
                employee.getRole().name()
        );
        String refreshToken = jwtTokenService.generateRefreshToken(
                employee.getCafeId(),
                employee.getId()
        );

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtTokenService.getAccessTokenExpirationSeconds(),
                employee.getId(),
                employee.getCafeId(),
                employee.getEmail(),
                employee.getRole()
        );
    }

    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenCommand command) {
        Objects.requireNonNull(command, "RefreshTokenCommand must not be null");

        JwtTokenClaims claims = jwtTokenService.parseToken(command.refreshToken());
        if (!"REFRESH".equals(claims.tokenType())) {
            throw new BusinessException("INVALID_TOKEN", "Token is not a refresh token", 401);
        }

        Employee employee = employeeRepository.findById(claims.employeeId())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Employee not found", 404));

        if (!employee.isActive()) {
            throw new BusinessException("EMPLOYEE_INACTIVE", "Employee account is inactive", 403);
        }

        UserAccount userAccount = userAccountRepository.findByEmployeeId(employee.getId())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User account not found", 404));

        if (userAccount.isLocked(Instant.now())) {
            throw new BusinessException("ACCOUNT_LOCKED", "Account is currently locked", 423);
        }

        String newAccessToken = jwtTokenService.generateAccessToken(
                employee.getCafeId(),
                employee.getId(),
                employee.getEmail(),
                employee.getRole().name()
        );
        String newRefreshToken = jwtTokenService.generateRefreshToken(
                employee.getCafeId(),
                employee.getId()
        );

        return new RefreshTokenResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                jwtTokenService.getAccessTokenExpirationSeconds()
        );
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(UUID employeeId) {
        if (employeeId == null) {
            throw new BusinessException("UNAUTHORIZED", "Authenticated employee ID is required", 401);
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Employee not found", 404));

        UserAccount userAccount = userAccountRepository.findByEmployeeId(employee.getId()).orElse(null);
        Instant lastLogin = userAccount != null ? userAccount.getLastLoginAt() : null;

        return new UserProfileResponse(
                employee.getId(),
                employee.getCafeId(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getRole(),
                employee.isActive(),
                lastLogin
        );
    }

    @Transactional
    public void changePassword(UUID employeeId, ChangePasswordCommand command) {
        if (employeeId == null) {
            throw new BusinessException("UNAUTHORIZED", "Authenticated employee ID is required", 401);
        }
        Objects.requireNonNull(command, "ChangePasswordCommand must not be null");

        UserAccount userAccount = userAccountRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User account not found", 404));

        if (!passwordEncodingService.matches(command.currentPassword(), userAccount.getPasswordHash())) {
            throw new BusinessException("INVALID_PASSWORD", "Current password does not match", 400);
        }

        if (command.currentPassword().equals(command.newPassword())) {
            throw new BusinessException("SAME_PASSWORD", "New password cannot be identical to current password", 400);
        }

        String encodedNewPassword = passwordEncodingService.encode(command.newPassword());
        userAccount.updatePassword(encodedNewPassword);
        userAccountRepository.save(userAccount);
    }
}
