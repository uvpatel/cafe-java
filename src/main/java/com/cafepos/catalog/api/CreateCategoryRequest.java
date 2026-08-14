package com.cafepos.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "Category name must not be blank")
        @Size(max = 100, message = "Category name must not exceed 100 characters")
        String name,

        @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "Color hex must be a valid 7-character hex string (e.g. #6c757d)")
        String colorHex,

        Integer sortOrder
) {
}
