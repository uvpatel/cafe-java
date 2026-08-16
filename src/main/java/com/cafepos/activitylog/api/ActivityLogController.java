package com.cafepos.activitylog.api;

import com.cafepos.activitylog.application.ActivityLogApplicationService;
import com.cafepos.activitylog.domain.ActivityLog;
import com.cafepos.shared.tenancy.TenantContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-logs")
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'AUDITOR')")
public class ActivityLogController {

    private final ActivityLogApplicationService activityLogApplicationService;

    public ActivityLogController(ActivityLogApplicationService activityLogApplicationService) {
        this.activityLogApplicationService = Objects.requireNonNull(activityLogApplicationService, "activityLogApplicationService must not be null");
    }

    @GetMapping
    public ResponseEntity<Page<ActivityLogResponse>> searchAuditLogs(
            @RequestParam(name = "action", required = false) String action,
            @RequestParam(name = "entityType", required = false) String entityType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        Page<ActivityLog> logs = activityLogApplicationService.searchActivityLogs(cafeId, action, entityType, pageable);
        Page<ActivityLogResponse> responsePage = logs.map(this::toResponse);
        return ResponseEntity.ok(responsePage);
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private ActivityLogResponse toResponse(ActivityLog log) {
        return new ActivityLogResponse(
                log.getId(),
                log.getCafeId(),
                log.getActorEmployeeId(),
                log.getActorEmail(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetailsJson(),
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }
}
