package com.cafepos.session.api;

import com.cafepos.session.application.ClosePosSessionUseCase;
import com.cafepos.session.application.GetCurrentSessionUseCase;
import com.cafepos.session.application.OpenPosSessionUseCase;
import com.cafepos.session.application.RecordCashMovementUseCase;
import com.cafepos.shared.error.BusinessException;
import com.cafepos.shared.security.UserPrincipal;
import com.cafepos.shared.tenancy.TenantContextHolder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pos-sessions")
public class PosSessionController {

    private final OpenPosSessionUseCase openPosSessionUseCase;
    private final ClosePosSessionUseCase closePosSessionUseCase;
    private final RecordCashMovementUseCase recordCashMovementUseCase;
    private final GetCurrentSessionUseCase getCurrentSessionUseCase;

    public PosSessionController(OpenPosSessionUseCase openPosSessionUseCase,
                                ClosePosSessionUseCase closePosSessionUseCase,
                                RecordCashMovementUseCase recordCashMovementUseCase,
                                GetCurrentSessionUseCase getCurrentSessionUseCase) {
        this.openPosSessionUseCase = Objects.requireNonNull(openPosSessionUseCase, "openPosSessionUseCase must not be null");
        this.closePosSessionUseCase = Objects.requireNonNull(closePosSessionUseCase, "closePosSessionUseCase must not be null");
        this.recordCashMovementUseCase = Objects.requireNonNull(recordCashMovementUseCase, "recordCashMovementUseCase must not be null");
        this.getCurrentSessionUseCase = Objects.requireNonNull(getCurrentSessionUseCase, "getCurrentSessionUseCase must not be null");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN')")
    public ResponseEntity<PosSessionResponse> openSession(@Valid @RequestBody OpenSessionRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        UUID employeeId = getAuthenticatedEmployeeId();
        PosSessionResponse response = openPosSessionUseCase.openSession(cafeId, employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN')")
    public ResponseEntity<PosSessionResponse> getCurrentSession(
            @RequestParam(name = "terminalId", required = false) UUID terminalId) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        UUID employeeId = getAuthenticatedEmployeeId();
        PosSessionResponse response = getCurrentSessionUseCase.getCurrentSession(cafeId, terminalId, employeeId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cash-movements")
    @PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN')")
    public ResponseEntity<CashMovementResponse> recordCashMovement(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RecordCashMovementRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        UUID employeeId = getAuthenticatedEmployeeId();
        CashMovementResponse response = recordCashMovementUseCase.recordCashMovement(cafeId, id, employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN')")
    public ResponseEntity<PosSessionResponse> closeSession(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CloseSessionRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        UUID employeeId = getAuthenticatedEmployeeId();
        PosSessionResponse response = closePosSessionUseCase.closeSession(cafeId, id, employeeId, request);
        return ResponseEntity.ok(response);
    }

    private UUID getAuthenticatedEmployeeId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getEmployeeId();
        }
        throw new BusinessException("UNAUTHORIZED", "Authenticated employee context is required", 401);
    }
}
