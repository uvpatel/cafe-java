package com.cafepos.catalog.application;

import com.cafepos.catalog.api.CatalogDtoMapper;
import com.cafepos.catalog.api.CreateProductRequest;
import com.cafepos.catalog.api.ProductResponse;
import com.cafepos.catalog.api.UpdateProductRequest;
import com.cafepos.catalog.domain.Product;
import com.cafepos.catalog.domain.ProductCategory;
import com.cafepos.catalog.domain.ProductCategoryRepository;
import com.cafepos.catalog.domain.ProductPricingValidator;
import com.cafepos.catalog.domain.ProductRepository;
import com.cafepos.catalog.domain.UnitOfMeasure;
import com.cafepos.shared.error.BusinessException;
import com.cafepos.shared.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductApplicationServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCategoryRepository categoryRepository;

    private final ProductPricingValidator pricingValidator = new ProductPricingValidator();

    private final CatalogDtoMapper dtoMapper = new CatalogDtoMapper();

    private ProductApplicationService service;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ProductApplicationService(productRepository, categoryRepository, pricingValidator, dtoMapper);
    }

    @Test
    @DisplayName("createProduct should validate category and create product successfully")
    void createProduct_success() {
        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Espresso", "ESP-001", new BigDecimal("3.50"), "USD", UnitOfMeasure.UNIT, true
        );
        ProductCategory category = new ProductCategory(categoryId, cafeId, "Beverages", "#FF0000", 1);
        Product product = new Product(productId, cafeId, categoryId, "Espresso", "ESP-001", Money.of(3.50), UnitOfMeasure.UNIT, true);

        when(categoryRepository.findByIdAndCafeId(categoryId, cafeId)).thenReturn(Optional.of(category));
        when(productRepository.existsByCafeIdAndSku(cafeId, "ESP-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = service.createProduct(cafeId, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.name()).isEqualTo("Espresso");
        assertThat(response.sku()).isEqualTo("ESP-001");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("createProduct should throw BusinessException when category not found")
    void createProduct_categoryNotFound() {
        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Espresso", "ESP-001", new BigDecimal("3.50"), "USD", UnitOfMeasure.UNIT, true
        );

        when(categoryRepository.findByIdAndCafeId(categoryId, cafeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createProduct(cafeId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    @DisplayName("createProduct should throw BusinessException when SKU is duplicate")
    void createProduct_duplicateSku() {
        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Espresso", "ESP-001", new BigDecimal("3.50"), "USD", UnitOfMeasure.UNIT, true
        );
        ProductCategory category = new ProductCategory(categoryId, cafeId, "Beverages", "#FF0000", 1);

        when(categoryRepository.findByIdAndCafeId(categoryId, cafeId)).thenReturn(Optional.of(category));
        when(productRepository.existsByCafeIdAndSku(cafeId, "ESP-001")).thenReturn(true);

        assertThatThrownBy(() -> service.createProduct(cafeId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Product with SKU 'ESP-001' already exists");
    }

    @Test
    @DisplayName("updateProduct should update details successfully")
    void updateProduct_success() {
        UpdateProductRequest request = new UpdateProductRequest(
                categoryId, "Super Espresso", "ESP-001", new BigDecimal("4.00"), "USD", UnitOfMeasure.UNIT, true
        );
        ProductCategory category = new ProductCategory(categoryId, cafeId, "Beverages", "#FF0000", 1);
        Product product = new Product(productId, cafeId, categoryId, "Espresso", "ESP-001", Money.of(3.50), UnitOfMeasure.UNIT, true);

        when(productRepository.findByIdAndCafeId(productId, cafeId)).thenReturn(Optional.of(product));
        when(categoryRepository.findByIdAndCafeId(categoryId, cafeId)).thenReturn(Optional.of(category));
        when(productRepository.save(product)).thenReturn(product);

        ProductResponse response = service.updateProduct(cafeId, productId, request);

        assertThat(response.name()).isEqualTo("Super Espresso");
        assertThat(response.priceAmount()).isEqualByComparingTo("4.0000");
    }

    @Test
    @DisplayName("archiveProduct should mark product as archived")
    void archiveProduct_success() {
        Product product = new Product(productId, cafeId, categoryId, "Espresso", "ESP-001", Money.of(3.50), UnitOfMeasure.UNIT, true);

        when(productRepository.findByIdAndCafeId(productId, cafeId)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        ProductResponse response = service.archiveProduct(cafeId, productId);

        assertThat(response.archived()).isTrue();
    }

    @Test
    @DisplayName("getProductsByCafe should return list of active products")
    void getProductsByCafe_success() {
        Product product = new Product(productId, cafeId, categoryId, "Espresso", "ESP-001", Money.of(3.50), UnitOfMeasure.UNIT, true);

        when(productRepository.findByCafeIdAndArchivedFalse(cafeId)).thenReturn(List.of(product));

        List<ProductResponse> responses = service.getProductsByCafe(cafeId, null, false);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).name()).isEqualTo("Espresso");
    }
}
