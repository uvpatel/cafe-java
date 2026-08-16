package com.cafepos.catalog.domain;

import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class CategoryValidationService {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6})$");
    private final ProductCategoryRepository categoryRepository;

    public CategoryValidationService(ProductCategoryRepository categoryRepository) {
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "ProductCategoryRepository must not be null");
    }

    public void validateCategoryCreation(UUID cafeId, String name, String colorHex) {
        ProductCategory.validateInputs(cafeId, name, colorHex != null && !colorHex.isBlank() ? colorHex : ProductCategory.DEFAULT_COLOR_HEX);
        if (categoryRepository.existsByCafeIdAndNameIgnoreCase(cafeId, name.trim())) {
            throw new IllegalArgumentException("Category with name '" + name.trim() + "' already exists for this cafe");
        }
    }

    public void validateCategoryUpdate(ProductCategory category, String newName, String newColorHex) {
        Objects.requireNonNull(category, "Category must not be null");
        String resolvedColor = newColorHex != null && !newColorHex.isBlank() ? newColorHex : ProductCategory.DEFAULT_COLOR_HEX;
        ProductCategory.validateInputs(category.getCafeId(), newName, resolvedColor);

        if (!category.getName().equalsIgnoreCase(newName.trim())
                && categoryRepository.existsByCafeIdAndNameIgnoreCase(category.getCafeId(), newName.trim())) {
            throw new IllegalArgumentException("Category with name '" + newName.trim() + "' already exists for this cafe");
        }
    }

    public boolean isValidHexColor(String colorHex) {
        if (colorHex == null || colorHex.isBlank()) {
            return false;
        }
        return HEX_COLOR_PATTERN.matcher(colorHex.trim()).matches();
    }
}
