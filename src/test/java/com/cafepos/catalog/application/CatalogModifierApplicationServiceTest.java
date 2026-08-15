package com.cafepos.catalog.application;

import com.cafepos.catalog.domain.ModifierGroup;
import com.cafepos.catalog.domain.ModifierGroupRepository;
import com.cafepos.catalog.domain.ModifierOption;
import com.cafepos.catalog.domain.ModifierOptionRepository;
import com.cafepos.shared.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogModifierApplicationServiceTest {

    @Mock
    private ModifierGroupRepository modifierGroupRepository;

    @Mock
    private ModifierOptionRepository modifierOptionRepository;

    @Mock
    private com.cafepos.catalog.domain.ProductRepository productRepository;

    private CatalogModifierApplicationService catalogModifierApplicationService;

    private final UUID cafeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        catalogModifierApplicationService = new CatalogModifierApplicationService(modifierGroupRepository, modifierOptionRepository, productRepository);
    }

    @Test
    @DisplayName("Should create modifier group successfully")
    void createModifierGroupSuccess() {
        when(modifierGroupRepository.save(any(ModifierGroup.class))).thenAnswer(i -> i.getArgument(0));

        ModifierGroup group = catalogModifierApplicationService.createModifierGroup(cafeId, "Syrups", 0, 3);

        assertThat(group).isNotNull();
        assertThat(group.getName()).isEqualTo("Syrups");
        assertThat(group.getMinSelection()).isEqualTo(0);
        assertThat(group.getMaxSelection()).isEqualTo(3);
        verify(modifierGroupRepository).save(any(ModifierGroup.class));
    }

    @Test
    @DisplayName("Should add option to modifier group successfully")
    void addOptionToGroupSuccess() {
        UUID groupId = UUID.randomUUID();
        ModifierGroup group = new ModifierGroup(groupId, cafeId, "Milk", 1, 1);

        when(modifierGroupRepository.findByIdAndCafeId(groupId, cafeId)).thenReturn(Optional.of(group));
        when(modifierOptionRepository.save(any(ModifierOption.class))).thenAnswer(i -> i.getArgument(0));

        ModifierOption option = catalogModifierApplicationService.addOptionToGroup(
                cafeId, groupId, "Almond Milk", new BigDecimal("0.8000"), false, 2
        );

        assertThat(option).isNotNull();
        assertThat(option.getName()).isEqualTo("Almond Milk");
        assertThat(option.getPriceDelta()).isEqualByComparingTo("0.8000");
        verify(modifierOptionRepository).save(any(ModifierOption.class));
    }

    @Test
    @DisplayName("Should throw BusinessException when modifier group not found")
    void addOptionGroupNotFoundThrows() {
        UUID groupId = UUID.randomUUID();
        when(modifierGroupRepository.findByIdAndCafeId(groupId, cafeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogModifierApplicationService.addOptionToGroup(cafeId, groupId, "Extra Shot", BigDecimal.ONE, false, 1))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getStatus())
                .isEqualTo(404);
    }

    @Test
    @DisplayName("Should link modifier group to product successfully")
    void linkModifierGroupToProductSuccess() {
        UUID productId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        com.cafepos.catalog.domain.Product product = new com.cafepos.catalog.domain.Product(
                productId, cafeId, categoryId, "Espresso", "ESP-01",
                new com.cafepos.shared.money.Money(new BigDecimal("3.50"), java.util.Currency.getInstance("USD")),
                com.cafepos.catalog.domain.UnitOfMeasure.UNIT, true
        );
        ModifierGroup group = new ModifierGroup(groupId, cafeId, "Syrups", 0, 2);

        when(productRepository.findByIdAndCafeId(productId, cafeId)).thenReturn(Optional.of(product));
        when(modifierGroupRepository.findByIdAndCafeId(groupId, cafeId)).thenReturn(Optional.of(group));
        when(productRepository.save(any(com.cafepos.catalog.domain.Product.class))).thenAnswer(i -> i.getArgument(0));

        com.cafepos.catalog.domain.Product updated = catalogModifierApplicationService.linkModifierGroupToProduct(cafeId, productId, groupId);

        assertThat(updated.getModifierGroupIds()).contains(groupId);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Should unlink modifier group from product successfully")
    void unlinkModifierGroupFromProductSuccess() {
        UUID productId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        com.cafepos.catalog.domain.Product product = new com.cafepos.catalog.domain.Product(
                productId, cafeId, categoryId, "Espresso", "ESP-01",
                new com.cafepos.shared.money.Money(new BigDecimal("3.50"), java.util.Currency.getInstance("USD")),
                com.cafepos.catalog.domain.UnitOfMeasure.UNIT, true
        );
        product.attachModifierGroup(groupId);

        when(productRepository.findByIdAndCafeId(productId, cafeId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(com.cafepos.catalog.domain.Product.class))).thenAnswer(i -> i.getArgument(0));

        com.cafepos.catalog.domain.Product updated = catalogModifierApplicationService.unlinkModifierGroupFromProduct(cafeId, productId, groupId);

        assertThat(updated.getModifierGroupIds()).doesNotContain(groupId);
        verify(productRepository).save(product);
    }
}

