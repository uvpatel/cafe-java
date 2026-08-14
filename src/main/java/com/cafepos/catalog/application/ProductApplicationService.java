package com.cafepos.catalog.application;

import com.cafepos.catalog.api.CatalogDtoMapper;
import com.cafepos.catalog.api.CreateProductRequest;
import com.cafepos.catalog.api.ProductResponse;
import com.cafepos.catalog.api.UpdateProductRequest;
import com.cafepos.catalog.domain.Product;
import com.cafepos.catalog.domain.ProductCategoryRepository;
import com.cafepos.catalog.domain.ProductPricingValidator;
import com.cafepos.catalog.domain.ProductRepository;
import com.cafepos.catalog.domain.UnitOfMeasure;
import com.cafepos.shared.error.BusinessException;
import com.cafepos.shared.money.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class ProductApplicationService implements ManageProductUseCase {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductPricingValidator pricingValidator;
    private final CatalogDtoMapper dtoMapper;

    public ProductApplicationService(ProductRepository productRepository,
                                     ProductCategoryRepository categoryRepository,
                                     ProductPricingValidator pricingValidator,
                                     CatalogDtoMapper dtoMapper) {
        this.productRepository = Objects.requireNonNull(productRepository, "productRepository must not be null");
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "categoryRepository must not be null");
        this.pricingValidator = Objects.requireNonNull(pricingValidator, "pricingValidator must not be null");
        this.dtoMapper = Objects.requireNonNull(dtoMapper, "dtoMapper must not be null");
    }

    @Override
    public ProductResponse createProduct(UUID cafeId, CreateProductRequest request) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        // Verify category exists and belongs to cafe
        categoryRepository.findByIdAndCafeId(request.categoryId(), cafeId)
                .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND", "Category not found with id " + request.categoryId(), 404));

        // Check duplicate SKU if SKU is provided
        if (request.sku() != null && !request.sku().isBlank()) {
            String trimmedSku = request.sku().trim();
            if (productRepository.existsByCafeIdAndSku(cafeId, trimmedSku)) {
                throw new BusinessException("DUPLICATE_SKU", "Product with SKU '" + trimmedSku + "' already exists for this cafe", 400);
            }
        }

        Money price;
        try {
            Currency currency = Currency.getInstance(request.currency().trim().toUpperCase());
            price = new Money(request.priceAmount(), currency);
            pricingValidator.validatePrice(price);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("PRODUCT_VALIDATION_FAILED", ex.getMessage(), 400);
        }

        UnitOfMeasure uom = request.unitOfMeasure() != null ? request.unitOfMeasure() : UnitOfMeasure.UNIT;
        boolean kitchenRouted = request.kitchenRouted() != null ? request.kitchenRouted() : true;

        try {
            Product product = new Product(
                    UUID.randomUUID(),
                    cafeId,
                    request.categoryId(),
                    request.name(),
                    request.sku(),
                    price,
                    uom,
                    kitchenRouted
            );
            Product saved = productRepository.save(product);
            return dtoMapper.toProductResponse(saved);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("PRODUCT_VALIDATION_FAILED", ex.getMessage(), 400);
        }
    }

    @Override
    public ProductResponse updateProduct(UUID cafeId, UUID productId, UpdateProductRequest request) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        Product product = productRepository.findByIdAndCafeId(productId, cafeId)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Product not found with id " + productId, 404));

        // Verify category exists and belongs to cafe
        categoryRepository.findByIdAndCafeId(request.categoryId(), cafeId)
                .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND", "Category not found with id " + request.categoryId(), 404));

        // Check duplicate SKU if SKU is provided and changed
        if (request.sku() != null && !request.sku().isBlank()) {
            String trimmedSku = request.sku().trim();
            if (!trimmedSku.equalsIgnoreCase(product.getSku())) {
                if (productRepository.existsByCafeIdAndSku(cafeId, trimmedSku)) {
                    throw new BusinessException("DUPLICATE_SKU", "Product with SKU '" + trimmedSku + "' already exists for this cafe", 400);
                }
            }
        }

        Money price;
        try {
            Currency currency = Currency.getInstance(request.currency().trim().toUpperCase());
            price = new Money(request.priceAmount(), currency);
            pricingValidator.validatePrice(price);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("PRODUCT_VALIDATION_FAILED", ex.getMessage(), 400);
        }

        UnitOfMeasure uom = request.unitOfMeasure() != null ? request.unitOfMeasure() : UnitOfMeasure.UNIT;
        boolean kitchenRouted = request.kitchenRouted() != null ? request.kitchenRouted() : product.isKitchenRouted();

        try {
            product.updateDetails(request.name(), request.sku(), request.categoryId(), price, uom, kitchenRouted);
            Product saved = productRepository.save(product);
            return dtoMapper.toProductResponse(saved);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("PRODUCT_VALIDATION_FAILED", ex.getMessage(), 400);
        }
    }

    @Override
    public ProductResponse archiveProduct(UUID cafeId, UUID productId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(productId, "productId must not be null");

        Product product = productRepository.findByIdAndCafeId(productId, cafeId)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Product not found with id " + productId, 404));

        product.archive();
        Product saved = productRepository.save(product);
        return dtoMapper.toProductResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCafe(UUID cafeId, UUID categoryId, boolean includeArchived) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");

        List<Product> products;
        if (categoryId != null) {
            products = productRepository.findByCafeIdAndCategoryId(cafeId, categoryId);
            if (!includeArchived) {
                products = products.stream().filter(p -> !p.isArchived()).toList();
            }
        } else {
            products = includeArchived
                    ? productRepository.findByCafeId(cafeId)
                    : productRepository.findByCafeIdAndArchivedFalse(cafeId);
        }

        return products.stream()
                .map(dtoMapper::toProductResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID cafeId, UUID productId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(productId, "productId must not be null");

        Product product = productRepository.findByIdAndCafeId(productId, cafeId)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Product not found with id " + productId, 404));

        return dtoMapper.toProductResponse(product);
    }
}
