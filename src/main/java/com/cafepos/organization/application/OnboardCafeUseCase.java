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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class OnboardCafeUseCase {

    private final CafeRepository cafeRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncodingService passwordEncodingService;

    public OnboardCafeUseCase(
            CafeRepository cafeRepository,
            EmployeeRepository employeeRepository,
            UserAccountRepository userAccountRepository,
            PasswordEncodingService passwordEncodingService) {
        this.cafeRepository = Objects.requireNonNull(cafeRepository, "cafeRepository must not be null");
        this.employeeRepository = Objects.requireNonNull(employeeRepository, "employeeRepository must not be null");
        this.userAccountRepository = Objects.requireNonNull(userAccountRepository, "userAccountRepository must not be null");
        this.passwordEncodingService = Objects.requireNonNull(passwordEncodingService, "passwordEncodingService must not be null");
    }

    @Transactional
    public OnboardCafeResponse execute(OnboardCafeCommand command) {
        Objects.requireNonNull(command, "OnboardCafeCommand must not be null");

        String normalizedSlug = command.slug() != null ? command.slug().trim().toLowerCase() : "";
        String normalizedEmail = command.adminEmail() != null ? command.adminEmail().trim().toLowerCase() : "";

        if (cafeRepository.existsBySlug(normalizedSlug)) {
            throw new BusinessException("DUPLICATE_SLUG", "Cafe slug '" + normalizedSlug + "' is already taken", 400);
        }

        Cafe cafe = new Cafe(
                UUID.randomUUID(),
                command.cafeName(),
                normalizedSlug,
                command.timezone(),
                command.currency()
        );
        Cafe savedCafe = cafeRepository.save(cafe);

        // Always enforce OWNER role for initial onboarding admin, ignoring any external role overrides
        Employee adminEmployee = new Employee(
                UUID.randomUUID(),
                savedCafe.getId(),
                command.adminName(),
                normalizedEmail,
                EmployeeRole.OWNER
        );
        Employee savedEmployee = employeeRepository.save(adminEmployee);

        String encodedPassword = passwordEncodingService.encode(command.adminPassword());
        UserAccount userAccount = new UserAccount(
                UUID.randomUUID(),
                savedEmployee.getId(),
                encodedPassword
        );
        userAccountRepository.save(userAccount);

        return new OnboardCafeResponse(
                savedCafe.getId(),
                savedEmployee.getId(),
                EmployeeRole.OWNER
        );
    }
}
