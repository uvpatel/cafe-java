package com.cafepos.catalog.api;

import com.cafepos.catalog.domain.Product;
import com.cafepos.catalog.domain.ProductCategory;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CatalogDtoMapper {

    public CategoryResponse toCategoryResponse(ProductCategory category) {
        if (category == null) {
            return null;
        }
        return new CategoryResponse(
                category.getId(),
                category.getCafeId(),
                category.getName(),
                category.getColorHex(),
                category.getSortOrder(),
                category.isArchived(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    public ProductResponse toProductResponse(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductResponse(
                product.getId(),
                product.getCafeId(),
                product.getCategoryId(),
                product.getName(),
                product.getSku(),
                product.getPrice().amount(),
                product.getPrice().currency().getCurrencyCode(),
                product.getUnitOfMeasure(),
                product.isKitchenRouted(),
                product.isArchived(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
