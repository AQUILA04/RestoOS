package com.resto.catalog.controller;

import com.resto.catalog.domain.Category;
import com.resto.catalog.domain.ModifierGroup;
import com.resto.catalog.domain.ModifierOption;
import com.resto.catalog.domain.Product;
import com.resto.catalog.service.CatalogService;
import com.resto.core.response.Response;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<Category> createCategory(@RequestBody CreateCategoryRequest request) {
        Category category = catalogService.createCategory(request.getOrganizationId(), request.getName(), request.getDisplayOrder());
        return Response.<Category>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(category)
                .build();
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER')")
    public Response<List<Category>> getCategories(@RequestParam("organizationId") UUID organizationId) {
        List<Category> categories = catalogService.getCategoriesByOrg(organizationId);
        return Response.<List<Category>>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(categories)
                .build();
    }

    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<Product> createProduct(@RequestBody CreateProductRequest request) {
        Product product = catalogService.createProduct(
                request.getOrganizationId(),
                request.getCategoryId(),
                request.getName(),
                request.getDescription(),
                request.getBasePrice(),
                request.getTaxRate(),
                request.getImageUrl()
        );
        return Response.<Product>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(product)
                .build();
    }

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER')")
    public Response<List<Product>> getProductsByCategory(@RequestParam("categoryId") UUID categoryId) {
        List<Product> products = catalogService.getProductsByCategory(categoryId);
        return Response.<List<Product>>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(products)
                .build();
    }

    @PostMapping("/modifier-groups")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<ModifierGroup> createModifierGroup(@RequestBody CreateModifierGroupRequest request) {
        ModifierGroup group = catalogService.createModifierGroup(
                request.getOrganizationId(),
                request.getName(),
                request.getMinSelection(),
                request.getMaxSelection(),
                request.getRequired()
        );
        return Response.<ModifierGroup>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(group)
                .build();
    }

    @PostMapping("/modifier-options")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<ModifierOption> addModifierOption(@RequestBody AddModifierOptionRequest request) {
        ModifierOption option = catalogService.addModifierOption(
                request.getOrganizationId(),
                request.getModifierGroupId(),
                request.getName(),
                request.getPriceDelta(),
                request.getDisplayOrder()
        );
        return Response.<ModifierOption>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(option)
                .build();
    }

    public static class CreateCategoryRequest {
        private UUID organizationId;
        private String name;
        private Integer displayOrder;

        public UUID getOrganizationId() { return organizationId; }
        public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    }

    public static class CreateProductRequest {
        private UUID organizationId;
        private UUID categoryId;
        private String name;
        private String description;
        private BigDecimal basePrice;
        private BigDecimal taxRate;
        private String imageUrl;

        public UUID getOrganizationId() { return organizationId; }
        public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
        public UUID getCategoryId() { return categoryId; }
        public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getBasePrice() { return basePrice; }
        public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
        public BigDecimal getTaxRate() { return taxRate; }
        public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }

    public static class CreateModifierGroupRequest {
        private UUID organizationId;
        private String name;
        private Integer minSelection;
        private Integer maxSelection;
        private Boolean required;

        public UUID getOrganizationId() { return organizationId; }
        public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getMinSelection() { return minSelection; }
        public void setMinSelection(Integer minSelection) { this.minSelection = minSelection; }
        public Integer getMaxSelection() { return maxSelection; }
        public void setMaxSelection(Integer maxSelection) { this.maxSelection = maxSelection; }
        public Boolean getRequired() { return required; }
        public void setRequired(Boolean required) { this.required = required; }
    }

    public static class AddModifierOptionRequest {
        private UUID organizationId;
        private UUID modifierGroupId;
        private String name;
        private BigDecimal priceDelta;
        private Integer displayOrder;

        public UUID getOrganizationId() { return organizationId; }
        public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
        public UUID getModifierGroupId() { return modifierGroupId; }
        public void setModifierGroupId(UUID modifierGroupId) { this.modifierGroupId = modifierGroupId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getPriceDelta() { return priceDelta; }
        public void setPriceDelta(BigDecimal priceDelta) { this.priceDelta = priceDelta; }
        public Integer getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    }
}
