package com.resto.catalog.service;

import com.resto.catalog.domain.Category;
import com.resto.catalog.domain.ModifierGroup;
import com.resto.catalog.domain.ModifierOption;
import com.resto.catalog.domain.Product;
import com.resto.catalog.domain.ProductModifierGroup;
import com.resto.catalog.repository.CategoryRepository;
import com.resto.catalog.repository.ModifierGroupRepository;
import com.resto.catalog.repository.ModifierOptionRepository;
import com.resto.catalog.repository.ProductModifierGroupRepository;
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
    private final ProductModifierGroupRepository productModifierGroupRepository;

    public CatalogService(CategoryRepository categoryRepository,
                          ProductRepository productRepository,
                          ModifierGroupRepository modifierGroupRepository,
                          ModifierOptionRepository modifierOptionRepository,
                          ProductModifierGroupRepository productModifierGroupRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.modifierGroupRepository = modifierGroupRepository;
        this.modifierOptionRepository = modifierOptionRepository;
        this.productModifierGroupRepository = productModifierGroupRepository;
    }

    public Category createCategory(UUID organizationId, String name, Integer displayOrder) {
        Category category = Category.builder()
                .organizationId(organizationId)
                .name(name)
                .displayOrder(displayOrder != null ? displayOrder : 0)
                .build();
        return categoryRepository.save(category);
    }

    public Category updateCategory(UUID categoryId, String name, Integer displayOrder, Boolean active) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
        if (name != null) category.setName(name);
        if (displayOrder != null) category.setDisplayOrder(displayOrder);
        if (active != null) category.setActive(active);
        return categoryRepository.save(category);
    }

    public void deleteCategory(UUID categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new IllegalArgumentException("Category not found: " + categoryId);
        }
        categoryRepository.deleteById(categoryId);
    }

    @Transactional(readOnly = true)
    public List<Category> getCategoriesByOrg(UUID organizationId) {
        return categoryRepository.findByOrganizationIdOrderByDisplayOrderAsc(organizationId);
    }

    public Product createProduct(UUID organizationId, UUID categoryId, String name, String description,
                                 BigDecimal basePrice, BigDecimal taxRate, String imageUrl) {
        return createProduct(organizationId, categoryId, name, description, basePrice, taxRate, imageUrl, null);
    }

    public Product createProduct(UUID organizationId, UUID categoryId, String name, String description,
                                 BigDecimal basePrice, BigDecimal taxRate, String imageUrl, Integer avgPrepMinutes) {
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
                .avgPrepMinutes(avgPrepMinutes != null ? avgPrepMinutes : 15)
                .build();
        return productRepository.save(product);
    }

    public Product updateProduct(UUID productId, UUID categoryId, String name, String description,
                                 BigDecimal basePrice, BigDecimal taxRate, String imageUrl, Boolean active) {
        return updateProduct(productId, categoryId, name, description, basePrice, taxRate, imageUrl, active, null);
    }

    public Product updateProduct(UUID productId, UUID categoryId, String name, String description,
                                 BigDecimal basePrice, BigDecimal taxRate, String imageUrl, Boolean active,
                                 Integer avgPrepMinutes) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        if (categoryId != null) product.setCategoryId(categoryId);
        if (name != null) product.setName(name);
        if (description != null) product.setDescription(description);
        if (basePrice != null) product.setBasePrice(basePrice);
        if (taxRate != null) product.setTaxRate(taxRate);
        if (imageUrl != null) product.setImageUrl(imageUrl);
        if (active != null) product.setActive(active);
        if (avgPrepMinutes != null) {
            if (avgPrepMinutes < 1) {
                throw new IllegalArgumentException("avgPrepMinutes must be >= 1");
            }
            product.setAvgPrepMinutes(avgPrepMinutes);
        }
        return productRepository.save(product);
    }

    public void deleteProduct(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }
        productRepository.deleteById(productId);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(UUID categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByOrg(UUID organizationId) {
        return productRepository.findByOrganizationId(organizationId);
    }

    public ModifierGroup createModifierGroup(UUID organizationId, String name, Integer minSelection,
                                             Integer maxSelection, Boolean required) {
        ModifierGroup group = ModifierGroup.builder()
                .organizationId(organizationId)
                .name(name)
                .minSelection(minSelection != null ? minSelection : 0)
                .maxSelection(maxSelection != null ? maxSelection : 1)
                .required(required != null ? required : false)
                .build();
        return modifierGroupRepository.save(group);
    }

    @Transactional(readOnly = true)
    public List<ModifierGroup> getModifierGroups(UUID organizationId) {
        return modifierGroupRepository.findAll().stream()
                .filter(g -> organizationId.equals(g.getOrganizationId()))
                .toList();
    }

    public ProductModifierGroup linkModifierGroupToProduct(UUID organizationId, UUID productId,
                                                          UUID modifierGroupId, Integer displayOrder) {
        if (!productRepository.existsById(productId)) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }
        if (!modifierGroupRepository.existsById(modifierGroupId)) {
            throw new IllegalArgumentException("ModifierGroup not found: " + modifierGroupId);
        }
        ProductModifierGroup link = new ProductModifierGroup();
        link.setProductId(productId);
        link.setModifierGroupId(modifierGroupId);
        link.setOrganizationId(organizationId);
        link.setDisplayOrder(displayOrder != null ? displayOrder : 0);
        return productModifierGroupRepository.save(link);
    }

    public ModifierOption addModifierOption(UUID organizationId, UUID modifierGroupId, String name,
                                            BigDecimal priceDelta, Integer displayOrder) {
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
