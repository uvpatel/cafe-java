package com.cafepos.identity.api;

import com.cafepos.identity.application.ShiftApplicationService;
import com.cafepos.shared.tenancy.TenantContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shifts")
public class EmployeeShiftController {

    private final ShiftApplicationService shiftApplicationService;

    public EmployeeShiftController(ShiftApplicationService shiftApplicationService) {
        this.shiftApplicationService = Objects.requireNonNull(shiftApplicationService, "shiftApplicationService must not be null");
    }

    @PostMapping("/clock-in")
    @PreAuthorize("hasAnyRole('CASHIER', 'WAITER', 'KITCHEN_STAFF', 'MANAGER', 'OWNER', 'ADMIN')")
    public ResponseEntity<EmployeeShiftResponse> clockIn(@RequestParam UUID employeeId, @RequestParam(required = false) String notes) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        EmployeeShiftResponse response = shiftApplicationService.clockIn(cafeId, employeeId, notes);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/start-break")
    @PreAuthorize("hasAnyRole('CASHIER', 'WAITER', 'KITCHEN_STAFF', 'MANAGER', 'OWNER', 'ADMIN')")
    public ResponseEntity<EmployeeShiftResponse> startBreak(@RequestParam UUID employeeId) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        EmployeeShiftResponse response = shiftApplicationService.startBreak(cafeId, employeeId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/end-break")
    @PreAuthorize("hasAnyRole('CASHIER', 'WAITER', 'KITCHEN_STAFF', 'MANAGER', 'OWNER', 'ADMIN')")
    public ResponseEntity<EmployeeShiftResponse> endBreak(@RequestParam UUID employeeId) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        EmployeeShiftResponse response = shiftApplicationService.endBreak(cafeId, employeeId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/clock-out")
    @PreAuthorize("hasAnyRole('CASHIER', 'WAITER', 'KITCHEN_STAFF', 'MANAGER', 'OWNER', 'ADMIN')")
    public ResponseEntity<EmployeeShiftResponse> clockOut(@RequestParam UUID employeeId) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        EmployeeShiftResponse response = shiftApplicationService.clockOut(cafeId, employeeId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/employees/{employeeId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER', 'ADMIN')")
    public ResponseEntity<List<EmployeeShiftResponse>> getEmployeeShifts(@PathVariable UUID employeeId) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        List<EmployeeShiftResponse> responses = shiftApplicationService.getShiftsForEmployee(cafeId, employeeId);
        return ResponseEntity.ok(responses);
    }
}
