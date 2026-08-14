package com.cafepos.catalog.application;

import com.cafepos.catalog.api.CreateProductRequest;
import com.cafepos.catalog.api.ProductResponse;
import com.cafepos.catalog.api.UpdateProductRequest;

import java.util.List;
import java.util.UUID;

public interface ManageProductUseCase {

    ProductResponse createProduct(UUID cafeId, CreateProductRequest request);

    ProductResponse updateProduct(UUID cafeId, UUID productId, UpdateProductRequest request);

    ProductResponse archiveProduct(UUID cafeId, UUID productId);

    List<ProductResponse> getProductsByCafe(UUID cafeId, UUID categoryId, boolean includeArchived);

    ProductResponse getProductById(UUID cafeId, UUID productId);
}
