package com.cafepos.catalog.application;

import com.cafepos.catalog.api.CatalogDtoMapper;
import com.cafepos.catalog.api.CategoryResponse;
import com.cafepos.catalog.api.CreateCategoryRequest;
import com.cafepos.catalog.api.UpdateCategoryRequest;
import com.cafepos.catalog.domain.CategoryValidationService;
import com.cafepos.catalog.domain.ProductCategory;
import com.cafepos.catalog.domain.ProductCategoryRepository;
import com.cafepos.shared.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryApplicationServiceTest {

    @Mock
    private ProductCategoryRepository categoryRepository;

    @Mock
    private CategoryValidationService categoryValidationService;

    private final CatalogDtoMapper dtoMapper = new CatalogDtoMapper();

    private CategoryApplicationService service;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CategoryApplicationService(categoryRepository, categoryValidationService, dtoMapper);
    }

    @Test
    @DisplayName("createCategory should validate and save category successfully")
    void createCategory_success() {
        CreateCategoryRequest request = new CreateCategoryRequest("Beverages", "#FF0000", 1);
        ProductCategory category = new ProductCategory(categoryId, cafeId, "Beverages", "#FF0000", 1);

        when(categoryRepository.save(any(ProductCategory.class))).thenReturn(category);

        CategoryResponse response = service.createCategory(cafeId, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(categoryId);
        assertThat(response.name()).isEqualTo("Beverages");
        verify(categoryValidationService).validateCategoryCreation(cafeId, "Beverages", "#FF0000");
        verify(categoryRepository).save(any(ProductCategory.class));
    }

    @Test
    @DisplayName("createCategory should throw BusinessException when validation fails")
    void createCategory_validationFails() {
        CreateCategoryRequest request = new CreateCategoryRequest("Beverages", "#FF0000", 1);
        doThrow(new IllegalArgumentException("Category with name 'Beverages' already exists"))
                .when(categoryValidationService).validateCategoryCreation(cafeId, "Beverages", "#FF0000");

        assertThatThrownBy(() -> service.createCategory(cafeId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("updateCategory should update existing category")
    void updateCategory_success() {
        ProductCategory existing = new ProductCategory(categoryId, cafeId, "Beverages", "#FF0000", 1);
        UpdateCategoryRequest request = new UpdateCategoryRequest("Cold Beverages", "#00FF00", 2);

        when(categoryRepository.findByIdAndCafeId(categoryId, cafeId)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(existing)).thenReturn(existing);

        CategoryResponse response = service.updateCategory(cafeId, categoryId, request);

        assertThat(response.name()).isEqualTo("Cold Beverages");
        assertThat(response.colorHex()).isEqualTo("#00FF00");
        assertThat(response.sortOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("archiveCategory should set archived flag to true")
    void archiveCategory_success() {
        ProductCategory existing = new ProductCategory(categoryId, cafeId, "Beverages", "#FF0000", 1);

        when(categoryRepository.findByIdAndCafeId(categoryId, cafeId)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(existing)).thenReturn(existing);

        CategoryResponse response = service.archiveCategory(cafeId, categoryId);

        assertThat(response.archived()).isTrue();
    }

    @Test
    @DisplayName("getCategoriesByCafe should return categories list")
    void getCategoriesByCafe_success() {
        ProductCategory cat = new ProductCategory(categoryId, cafeId, "Beverages", "#FF0000", 1);
        when(categoryRepository.findByCafeIdAndArchivedFalseOrderBySortOrderAsc(cafeId)).thenReturn(List.of(cat));

        List<CategoryResponse> responses = service.getCategoriesByCafe(cafeId, false);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).name()).isEqualTo("Beverages");
    }
}
