package com.cafepos.catalog.api;

import com.cafepos.catalog.application.CatalogModifierApplicationService;
import com.cafepos.catalog.domain.ModifierGroup;
import com.cafepos.catalog.domain.ModifierOption;
import com.cafepos.shared.tenancy.TenantContextHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog/modifiers")
@PreAuthorize("hasAnyRole('CASHIER', 'OWNER', 'ADMIN')")
public class ModifierGroupController {

    private final CatalogModifierApplicationService catalogModifierApplicationService;

    public ModifierGroupController(CatalogModifierApplicationService catalogModifierApplicationService) {
        this.catalogModifierApplicationService = Objects.requireNonNull(catalogModifierApplicationService, "catalogModifierApplicationService must not be null");
    }

    public record CreateModifierGroupRequest(
            @NotBlank String name,
            @Min(0) int minSelection,
            @Min(0) int maxSelection
    ) {}

    public record CreateModifierOptionRequest(
            @NotBlank String name,
            @NotNull BigDecimal priceDelta,
            boolean isDefault,
            int sortOrder
    ) {}

    @PostMapping("/groups")
    public ResponseEntity<ModifierGroup> createGroup(@Valid @RequestBody CreateModifierGroupRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        ModifierGroup group = catalogModifierApplicationService.createModifierGroup(cafeId, request.name(), request.minSelection(), request.maxSelection());
        return ResponseEntity.status(HttpStatus.CREATED).body(group);
    }

    @GetMapping("/groups")
    public ResponseEntity<List<ModifierGroup>> getGroups() {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        return ResponseEntity.ok(catalogModifierApplicationService.getModifierGroups(cafeId));
    }

    @PostMapping("/groups/{groupId}/options")
    public ResponseEntity<ModifierOption> addOption(@PathVariable("groupId") UUID groupId, @Valid @RequestBody CreateModifierOptionRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        ModifierOption option = catalogModifierApplicationService.addOptionToGroup(
                cafeId, groupId, request.name(), request.priceDelta(), request.isDefault(), request.sortOrder()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(option);
    }

    @GetMapping("/groups/{groupId}/options")
    public ResponseEntity<List<ModifierOption>> getOptions(@PathVariable("groupId") UUID groupId) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        return ResponseEntity.ok(catalogModifierApplicationService.getOptionsForGroup(cafeId, groupId));
    }

    @PostMapping("/products/{productId}/groups/{groupId}")
    public ResponseEntity<Void> linkGroupToProduct(@PathVariable("productId") UUID productId, @PathVariable("groupId") UUID groupId) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        catalogModifierApplicationService.linkModifierGroupToProduct(cafeId, productId, groupId);
        return ResponseEntity.noContent().build();
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/products/{productId}/groups/{groupId}")
    public ResponseEntity<Void> unlinkGroupFromProduct(@PathVariable("productId") UUID productId, @PathVariable("groupId") UUID groupId) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        catalogModifierApplicationService.unlinkModifierGroupFromProduct(cafeId, productId, groupId);
        return ResponseEntity.noContent().build();
    }
}

