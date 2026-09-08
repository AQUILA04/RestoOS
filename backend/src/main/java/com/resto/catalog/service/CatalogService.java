package com.resto.catalog.service;

import com.resto.catalog.domain.Category;
import com.resto.catalog.domain.ModifierGroup;
import com.resto.catalog.domain.ModifierOption;
import com.resto.catalog.domain.Product;
import com.resto.catalog.repository.CategoryRepository;
import com.resto.catalog.repository.ModifierGroupRepository;
import com.resto.catalog.repository.ModifierOptionRepository;
import com.resto.catalog.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ModifierGroupRepository modifierGroupRepository;
    private final ModifierOptionRepository modifierOptionRepository;

    public CatalogService(CategoryRepository categoryRepository,
                          ProductRepository productRepository,
                          ModifierGroupRepository modifierGroupRepository,
                          ModifierOptionRepository modifierOptionRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.modifierGroupRepository = modifierGroupRepository;
        this.modifierOptionRepository = modifierOptionRepository;
    }

    public Category createCategory(UUID organizationId, String name, Integer displayOrder) {
        Category category = Category.builder()
                .organizationId(organizationId)
                .name(name)
                .displayOrder(displayOrder != null ? displayOrder : 0)
                .build();
        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public List<Category> getCategoriesByOrg(UUID organizationId) {
        return categoryRepository.findByOrganizationIdOrderByDisplayOrderAsc(organizationId);
    }

    public Product createProduct(UUID organizationId, UUID categoryId, String name, String description, BigDecimal basePrice, BigDecimal taxRate, String imageUrl) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new IllegalArgumentException("Category not found: " + categoryId);
        }
        Product product = Product.builder()
                .organizationId(organizationId)
                .categoryId(categoryId)
                .name(name)
                .description(description)
                .basePrice(basePrice)
                .taxRate(taxRate != null ? taxRate : new BigDecimal("10.00"))
                .imageUrl(imageUrl)
                .build();
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(UUID categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    public ModifierGroup createModifierGroup(UUID organizationId, String name, Integer minSelection, Integer maxSelection, Boolean required) {
        ModifierGroup group = ModifierGroup.builder()
                .organizationId(organizationId)
                .name(name)
                .minSelection(minSelection != null ? minSelection : 0)
                .maxSelection(maxSelection != null ? maxSelection : 1)
                .required(required != null ? required : false)
                .build();
        return modifierGroupRepository.save(group);
    }

    public ModifierOption addModifierOption(UUID organizationId, UUID modifierGroupId, String name, BigDecimal priceDelta, Integer displayOrder) {
        if (!modifierGroupRepository.existsById(modifierGroupId)) {
            throw new IllegalArgumentException("ModifierGroup not found: " + modifierGroupId);
        }
        ModifierOption option = ModifierOption.builder()
                .organizationId(organizationId)
                .modifierGroupId(modifierGroupId)
                .name(name)
                .priceDelta(priceDelta != null ? priceDelta : BigDecimal.ZERO)
                .displayOrder(displayOrder != null ? displayOrder : 0)
                .build();
        return modifierOptionRepository.save(option);
    }
}
