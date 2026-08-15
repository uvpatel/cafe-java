package com.cafepos.floor.api;

import com.cafepos.floor.application.FloorApplicationService;
import com.cafepos.floor.domain.DiningTable;
import com.cafepos.shared.tenancy.TenantContextHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tables")
@PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN')")
public class DiningTableController {

    private final FloorApplicationService floorApplicationService;

    public DiningTableController(FloorApplicationService floorApplicationService) {
        this.floorApplicationService = Objects.requireNonNull(floorApplicationService, "floorApplicationService must not be null");
    }

    public record MergeTablesRequest(
            @NotNull UUID targetTableId
    ) {}

    @PostMapping("/{id}/merge")
    public ResponseEntity<DiningTable> mergeTables(@PathVariable("id") UUID id, @Valid @RequestBody MergeTablesRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        DiningTable table = floorApplicationService.mergeTables(cafeId, id, request.targetTableId());
        return ResponseEntity.ok(table);
    }

    @PostMapping("/{id}/unmerge")
    public ResponseEntity<DiningTable> unmergeTable(@PathVariable("id") UUID id) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        DiningTable table = floorApplicationService.unmergeTable(cafeId, id);
        return ResponseEntity.ok(table);
    }

    @GetMapping("/{id}/merged-children")
    public ResponseEntity<List<DiningTable>> getMergedChildren(@PathVariable("id") UUID id) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        List<DiningTable> mergedTables = floorApplicationService.getMergedTablesForTarget(cafeId, id);
        return ResponseEntity.ok(mergedTables);
    }
}
