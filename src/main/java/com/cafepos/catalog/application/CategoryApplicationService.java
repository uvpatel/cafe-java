package com.cafepos.catalog.application;

import com.cafepos.catalog.api.CatalogDtoMapper;
import com.cafepos.catalog.api.CategoryResponse;
import com.cafepos.catalog.api.CreateCategoryRequest;
import com.cafepos.catalog.api.UpdateCategoryRequest;
import com.cafepos.catalog.domain.CategoryValidationService;
import com.cafepos.catalog.domain.ProductCategory;
import com.cafepos.catalog.domain.ProductCategoryRepository;
import com.cafepos.shared.error.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class CategoryApplicationService implements ManageCategoryUseCase {

    private final ProductCategoryRepository categoryRepository;
    private final CategoryValidationService categoryValidationService;
    private final CatalogDtoMapper dtoMapper;

    public CategoryApplicationService(ProductCategoryRepository categoryRepository,
                                       CategoryValidationService categoryValidationService,
                                       CatalogDtoMapper dtoMapper) {
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "categoryRepository must not be null");
        this.categoryValidationService = Objects.requireNonNull(categoryValidationService, "categoryValidationService must not be null");
        this.dtoMapper = Objects.requireNonNull(dtoMapper, "dtoMapper must not be null");
    }

    @Override
    public CategoryResponse createCategory(UUID cafeId, CreateCategoryRequest request) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        String colorHex = request.colorHex() != null && !request.colorHex().isBlank()
                ? request.colorHex()
                : ProductCategory.DEFAULT_COLOR_HEX;
        int sortOrder = request.sortOrder() != null ? request.sortOrder() : 0;

        try {
            categoryValidationService.validateCategoryCreation(cafeId, request.name(), colorHex);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("CATEGORY_VALIDATION_FAILED", ex.getMessage(), 400);
        }

        ProductCategory category = new ProductCategory(cafeId, request.name(), colorHex, sortOrder);
        ProductCategory saved = categoryRepository.save(category);
        return dtoMapper.toCategoryResponse(saved);
    }

    @Override
    public CategoryResponse updateCategory(UUID cafeId, UUID categoryId, UpdateCategoryRequest request) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(categoryId, "categoryId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        ProductCategory category = categoryRepository.findByIdAndCafeId(categoryId, cafeId)
                .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND", "Category not found with id " + categoryId, 404));

        String colorHex = request.colorHex() != null && !request.colorHex().isBlank()
                ? request.colorHex()
                : ProductCategory.DEFAULT_COLOR_HEX;
        int sortOrder = request.sortOrder() != null ? request.sortOrder() : category.getSortOrder();

        try {
            categoryValidationService.validateCategoryUpdate(category, request.name(), colorHex);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("CATEGORY_VALIDATION_FAILED", ex.getMessage(), 400);
        }

        category.update(request.name(), colorHex, sortOrder);
        ProductCategory saved = categoryRepository.save(category);
        return dtoMapper.toCategoryResponse(saved);
    }

    @Override
    public CategoryResponse archiveCategory(UUID cafeId, UUID categoryId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(categoryId, "categoryId must not be null");

        ProductCategory category = categoryRepository.findByIdAndCafeId(categoryId, cafeId)
                .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND", "Category not found with id " + categoryId, 404));

        category.archive();
        ProductCategory saved = categoryRepository.save(category);
        return dtoMapper.toCategoryResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesByCafe(UUID cafeId, boolean includeArchived) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");

        List<ProductCategory> categories = includeArchived
                ? categoryRepository.findByCafeIdOrderBySortOrderAsc(cafeId)
                : categoryRepository.findByCafeIdAndArchivedFalseOrderBySortOrderAsc(cafeId);

        return categories.stream()
                .map(dtoMapper::toCategoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID cafeId, UUID categoryId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(categoryId, "categoryId must not be null");

        ProductCategory category = categoryRepository.findByIdAndCafeId(categoryId, cafeId)
                .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND", "Category not found with id " + categoryId, 404));

        return dtoMapper.toCategoryResponse(category);
    }
}
