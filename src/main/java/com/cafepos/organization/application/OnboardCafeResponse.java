package com.cafepos.organization.application;

import com.cafepos.identity.domain.EmployeeRole;

import java.util.UUID;

public record OnboardCafeResponse(
        UUID cafeId,
        UUID employeeId,
        EmployeeRole adminRole
) {}
