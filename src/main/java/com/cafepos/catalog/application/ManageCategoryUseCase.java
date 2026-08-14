package com.cafepos.catalog.application;

import com.cafepos.catalog.api.CategoryResponse;
import com.cafepos.catalog.api.CreateCategoryRequest;
import com.cafepos.catalog.api.UpdateCategoryRequest;

import java.util.List;
import java.util.UUID;

public interface ManageCategoryUseCase {

    CategoryResponse createCategory(UUID cafeId, CreateCategoryRequest request);

    CategoryResponse updateCategory(UUID cafeId, UUID categoryId, UpdateCategoryRequest request);

    CategoryResponse archiveCategory(UUID cafeId, UUID categoryId);

    List<CategoryResponse> getCategoriesByCafe(UUID cafeId, boolean includeArchived);

    CategoryResponse getCategoryById(UUID cafeId, UUID categoryId);
}
