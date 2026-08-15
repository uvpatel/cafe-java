package com.cafepos.catalog.application;

import com.cafepos.catalog.domain.ModifierGroup;
import com.cafepos.catalog.domain.ModifierGroupRepository;
import com.cafepos.catalog.domain.ModifierOption;
import com.cafepos.catalog.domain.ModifierOptionRepository;
import com.cafepos.catalog.domain.Product;
import com.cafepos.catalog.domain.ProductRepository;
import com.cafepos.shared.error.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class CatalogModifierApplicationService {

    private final ModifierGroupRepository modifierGroupRepository;
    private final ModifierOptionRepository modifierOptionRepository;
    private final ProductRepository productRepository;

    public CatalogModifierApplicationService(ModifierGroupRepository modifierGroupRepository,
                                             ModifierOptionRepository modifierOptionRepository,
                                             ProductRepository productRepository) {
        this.modifierGroupRepository = Objects.requireNonNull(modifierGroupRepository, "modifierGroupRepository must not be null");
        this.modifierOptionRepository = Objects.requireNonNull(modifierOptionRepository, "modifierOptionRepository must not be null");
        this.productRepository = Objects.requireNonNull(productRepository, "productRepository must not be null");
    }

    public ModifierGroup createModifierGroup(UUID cafeId, String name, int minSelection, int maxSelection) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        ModifierGroup group = new ModifierGroup(cafeId, name, minSelection, maxSelection);
        return modifierGroupRepository.save(group);
    }

    public ModifierOption addOptionToGroup(UUID cafeId, UUID groupId, String name, BigDecimal priceDelta, boolean isDefault, int sortOrder) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(groupId, "groupId must not be null");

        ModifierGroup group = modifierGroupRepository.findByIdAndCafeId(groupId, cafeId)
                .orElseThrow(() -> new BusinessException("MODIFIER_GROUP_NOT_FOUND", "Modifier group not found for ID: " + groupId, 404));

        ModifierOption option = new ModifierOption(group.getId(), name, priceDelta, isDefault, sortOrder);
        return modifierOptionRepository.save(option);
    }

    public Product linkModifierGroupToProduct(UUID cafeId, UUID productId, UUID groupId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(groupId, "groupId must not be null");

        Product product = productRepository.findByIdAndCafeId(productId, cafeId)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Product not found for ID: " + productId, 404));

        ModifierGroup group = modifierGroupRepository.findByIdAndCafeId(groupId, cafeId)
                .orElseThrow(() -> new BusinessException("MODIFIER_GROUP_NOT_FOUND", "Modifier group not found for ID: " + groupId, 404));

        product.attachModifierGroup(group.getId());
        return productRepository.save(product);
    }

    public Product unlinkModifierGroupFromProduct(UUID cafeId, UUID productId, UUID groupId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(groupId, "groupId must not be null");

        Product product = productRepository.findByIdAndCafeId(productId, cafeId)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Product not found for ID: " + productId, 404));

        product.detachModifierGroup(groupId);
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<ModifierGroup> getModifierGroups(UUID cafeId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        return modifierGroupRepository.findByCafeId(cafeId);
    }

    @Transactional(readOnly = true)
    public List<ModifierOption> getOptionsForGroup(UUID cafeId, UUID groupId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(groupId, "groupId must not be null");

        modifierGroupRepository.findByIdAndCafeId(groupId, cafeId)
                .orElseThrow(() -> new BusinessException("MODIFIER_GROUP_NOT_FOUND", "Modifier group not found for ID: " + groupId, 404));

        return modifierOptionRepository.findByGroupIdOrderBySortOrderAsc(groupId);
    }
}

