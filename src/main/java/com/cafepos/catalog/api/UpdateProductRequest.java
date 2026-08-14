package com.cafepos.catalog.api;

import com.cafepos.catalog.domain.UnitOfMeasure;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductRequest(
        @NotNull(message = "Category ID must not be null")
        UUID categoryId,

        @NotBlank(message = "Product name must not be blank")
        @Size(max = 100, message = "Product name must not exceed 100 characters")
        String name,

        @Size(max = 50, message = "SKU must not exceed 50 characters")
        String sku,

        @NotNull(message = "Price amount must not be null")
        @DecimalMin(value = "0.0", message = "Price amount cannot be negative")
        BigDecimal priceAmount,

        @NotBlank(message = "Currency must not be blank")
        @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
        String currency,

        UnitOfMeasure unitOfMeasure,

        Boolean kitchenRouted
) {
}
