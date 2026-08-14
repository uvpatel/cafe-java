package com.cafepos.catalog.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class CategoryValidationServiceTest {

    @Mock
    private ProductCategoryRepository categoryRepository;

    private CategoryValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new CategoryValidationService(categoryRepository);
    }

    @Test
    @DisplayName("Should pass category creation validation for unique name")
    void shouldPassCreationForUniqueName() {
        UUID cafeId = UUID.randomUUID();
        given(categoryRepository.existsByCafeIdAndNameIgnoreCase(cafeId, "Beverages")).willReturn(false);

        validationService.validateCategoryCreation(cafeId, "Beverages", "#FF5733");

        verify(categoryRepository).existsByCafeIdAndNameIgnoreCase(cafeId, "Beverages");
    }

    @Test
    @DisplayName("Should throw exception when creating category with duplicate name")
    void shouldThrowWhenDuplicateNameOnCreate() {
        UUID cafeId = UUID.randomUUID();
        given(categoryRepository.existsByCafeIdAndNameIgnoreCase(cafeId, "Beverages")).willReturn(true);

        assertThatThrownBy(() -> validationService.validateCategoryCreation(cafeId, "Beverages", "#FF5733"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category with name 'Beverages' already exists");
    }

    @Test
    @DisplayName("Should pass category update validation when name unchanged")
    void shouldPassUpdateWhenNameUnchanged() {
        UUID cafeId = UUID.randomUUID();
        ProductCategory category = new ProductCategory(cafeId, "Beverages", "#112233", 1);

        validationService.validateCategoryUpdate(category, "Beverages", "#445566");
    }

    @Test
    @DisplayName("Should throw exception when updating category to existing duplicate name")
    void shouldThrowWhenUpdateToDuplicateName() {
        UUID cafeId = UUID.randomUUID();
        ProductCategory category = new ProductCategory(cafeId, "Beverages", "#112233", 1);

        given(categoryRepository.existsByCafeIdAndNameIgnoreCase(cafeId, "Snacks")).willReturn(true);

        assertThatThrownBy(() -> validationService.validateCategoryUpdate(category, "Snacks", "#445566"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category with name 'Snacks' already exists");
    }

    @Test
    @DisplayName("Should validate hex color string correctly")
    void shouldValidateHexColor() {
        assertThat(validationService.isValidHexColor("#123456")).isTrue();
        assertThat(validationService.isValidHexColor("#ABCDEF")).isTrue();
        assertThat(validationService.isValidHexColor("invalid")).isFalse();
        assertThat(validationService.isValidHexColor(null)).isFalse();
    }
}
