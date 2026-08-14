package com.cafepos.catalog.api;

import com.cafepos.catalog.application.ManageProductUseCase;
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
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ManageProductUseCase manageProductUseCase;

    public ProductController(ManageProductUseCase manageProductUseCase) {
        this.manageProductUseCase = Objects.requireNonNull(manageProductUseCase, "manageProductUseCase must not be null");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MANAGER', 'CASHIER', 'KITCHEN_STAFF')")
    public ResponseEntity<List<ProductResponse>> getProducts(
            @RequestParam(name = "categoryId", required = false) UUID categoryId,
            @RequestParam(name = "includeArchived", defaultValue = "false") boolean includeArchived) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        List<ProductResponse> products = manageProductUseCase.getProductsByCafe(cafeId, categoryId, includeArchived);
        return ResponseEntity.ok(products);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        ProductResponse response = manageProductUseCase.createProduct(cafeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        ProductResponse response = manageProductUseCase.updateProduct(cafeId, id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ProductResponse> archiveProduct(@PathVariable("id") UUID id) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        ProductResponse response = manageProductUseCase.archiveProduct(cafeId, id);
        return ResponseEntity.ok(response);
    }
}
