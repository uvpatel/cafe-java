package com.cafepos.catalog.api;

import com.cafepos.catalog.application.ManageCategoryUseCase;
import com.cafepos.shared.tenancy.TenantContextHolder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
public class ProductCategoryController {

    private final ManageCategoryUseCase manageCategoryUseCase;

    public ProductCategoryController(ManageCategoryUseCase manageCategoryUseCase) {
        this.manageCategoryUseCase = Objects.requireNonNull(manageCategoryUseCase, "manageCategoryUseCase must not be null");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MANAGER', 'CASHIER', 'KITCHEN_STAFF')")
    public ResponseEntity<List<CategoryResponse>> getCategories(
            @RequestParam(name = "includeArchived", defaultValue = "false") boolean includeArchived) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        List<CategoryResponse> categories = manageCategoryUseCase.getCategoriesByCafe(cafeId, includeArchived);
        return ResponseEntity.ok(categories);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        CategoryResponse response = manageCategoryUseCase.createCategory(cafeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        CategoryResponse response = manageCategoryUseCase.updateCategory(cafeId, id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<CategoryResponse> archiveCategory(@PathVariable("id") UUID id) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        CategoryResponse response = manageCategoryUseCase.archiveCategory(cafeId, id);
        return ResponseEntity.ok(response);
    }
}
