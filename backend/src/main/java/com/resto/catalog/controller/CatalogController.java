package com.resto.catalog.controller;

import com.resto.catalog.domain.Category;
import com.resto.catalog.domain.ModifierGroup;
import com.resto.catalog.domain.ModifierOption;
import com.resto.catalog.domain.Product;
import com.resto.catalog.domain.ProductModifierGroup;
import com.resto.catalog.service.CatalogService;
import com.resto.core.response.Response;
import com.resto.core.security.JwtAuth;
import com.resto.core.security.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping({"/api/v1/categories", "/api/v1/catalog/categories"})
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<Category> createCategory(@RequestBody CreateCategoryRequest request) {
        UUID organizationId = resolveOrg(request.getOrganizationId());
        Integer order = request.getSortOrder() != null ? request.getSortOrder() : request.getDisplayOrder();
        return ok(catalogService.createCategory(organizationId, request.getName(), order));
    }

    @PutMapping({"/api/v1/categories/{id}", "/api/v1/catalog/categories/{id}"})
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<Category> updateCategory(@PathVariable("id") UUID id, @RequestBody CreateCategoryRequest request) {
        return ok(catalogService.updateCategory(id, request.getName(),
                request.getSortOrder() != null ? request.getSortOrder() : request.getDisplayOrder(),
                request.getActive()));
    }

    @DeleteMapping({"/api/v1/categories/{id}", "/api/v1/catalog/categories/{id}"})
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<String> deleteCategory(@PathVariable("id") UUID id) {
        catalogService.deleteCategory(id);
        return ok("deleted");
    }

    @GetMapping({"/api/v1/categories", "/api/v1/catalog/categories"})
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER')")
    public Response<List<Category>> getCategories(
            @RequestParam(value = "organizationId", required = false) UUID organizationId) {
        UUID orgId = organizationId != null ? organizationId : resolveOrg(null);
        return ok(catalogService.getCategoriesByOrg(orgId));
    }

    @PostMapping({"/api/v1/products", "/api/v1/catalog/products"})
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<Product> createProduct(@RequestBody CreateProductRequest request) {
        UUID organizationId = resolveOrg(request.getOrganizationId());
        return ok(catalogService.createProduct(
                organizationId, request.getCategoryId(), request.getName(), request.getDescription(),
                request.getBasePrice(), request.getTaxRate(), request.getImageUrl(), request.getAvgPrepMinutes()
        ));
    }

    @PutMapping({"/api/v1/products/{id}", "/api/v1/catalog/products/{id}"})
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<Product> updateProduct(@PathVariable("id") UUID id, @RequestBody CreateProductRequest request) {
        return ok(catalogService.updateProduct(id, request.getCategoryId(), request.getName(),
                request.getDescription(), request.getBasePrice(), request.getTaxRate(),
                request.getImageUrl(), request.getActive(), request.getAvgPrepMinutes()));
    }

    @DeleteMapping({"/api/v1/products/{id}", "/api/v1/catalog/products/{id}"})
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<String> deleteProduct(@PathVariable("id") UUID id) {
        catalogService.deleteProduct(id);
        return ok("deleted");
    }

    @GetMapping({"/api/v1/products", "/api/v1/catalog/products"})
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER')")
    public Response<List<Product>> getProducts(
            @RequestParam(value = "categoryId", required = false) UUID categoryId,
            @RequestParam(value = "organizationId", required = false) UUID organizationId) {
        if (categoryId != null) {
            return ok(catalogService.getProductsByCategory(categoryId));
        }
        return ok(catalogService.getProductsByOrg(resolveOrg(organizationId)));
    }

    @PostMapping({"/api/v1/modifier-groups", "/api/v1/catalog/modifier-groups"})
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<ModifierGroup> createModifierGroup(@RequestBody CreateModifierGroupRequest request) {
        UUID organizationId = resolveOrg(request.getOrganizationId());
        ModifierGroup group = catalogService.createModifierGroup(
                organizationId, request.getName(), request.getMinSelection(),
                request.getMaxSelection(), request.getRequired()
        );
        if (request.getProductId() != null) {
            catalogService.linkModifierGroupToProduct(organizationId, request.getProductId(),
                    group.getId(), 0);
        }
        return ok(group);
    }

    @GetMapping({"/api/v1/modifier-groups", "/api/v1/catalog/modifier-groups"})
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<List<ModifierGroup>> getModifierGroups(
            @RequestParam(value = "organizationId", required = false) UUID organizationId) {
        return ok(catalogService.getModifierGroups(resolveOrg(organizationId)));
    }

    @PostMapping({"/api/v1/products/{productId}/modifier-groups/{groupId}",
            "/api/v1/catalog/products/{productId}/modifier-groups/{groupId}"})
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<ProductModifierGroup> linkModifier(@PathVariable UUID productId,
                                                      @PathVariable UUID groupId) {
        UUID orgId = resolveOrg(null);
        return ok(catalogService.linkModifierGroupToProduct(orgId, productId, groupId, 0));
    }

    @PostMapping({"/api/v1/modifier-options", "/api/v1/catalog/modifier-options"})
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<ModifierOption> addModifierOption(@RequestBody AddModifierOptionRequest request) {
        UUID organizationId = resolveOrg(request.getOrganizationId());
        return ok(catalogService.addModifierOption(
                organizationId, request.getModifierGroupId(), request.getName(),
                request.getPriceDelta(), request.getDisplayOrder()
        ));
    }

    private UUID resolveOrg(UUID requestOrg) {
        if (TenantContext.getOrgId() != null) return TenantContext.getOrgId();
        try {
            return JwtAuth.organizationId();
        } catch (Exception e) {
            if (requestOrg != null) return requestOrg;
            throw new IllegalStateException("organization_id required");
        }
    }

    private <T> Response<T> ok(T data) {
        return Response.<T>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(data)
                .build();
    }

    public static class CreateCategoryRequest {
        private UUID organizationId;
        private String name;
        private Integer displayOrder;
        private Integer sortOrder;
        private Boolean active;
        public UUID getOrganizationId() { return organizationId; }
        public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }

    public static class CreateProductRequest {
        private UUID organizationId;
        private UUID categoryId;
        private String name;
        private String description;
        private BigDecimal basePrice;
        private BigDecimal taxRate;
        private String imageUrl;
        private Boolean active;
        private Integer avgPrepMinutes;
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
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
        public Integer getAvgPrepMinutes() { return avgPrepMinutes; }
        public void setAvgPrepMinutes(Integer avgPrepMinutes) { this.avgPrepMinutes = avgPrepMinutes; }
    }

    public static class CreateModifierGroupRequest {
        private UUID organizationId;
        private UUID productId;
        private String name;
        private Integer minSelection;
        private Integer maxSelection;
        private Boolean required;
        public UUID getOrganizationId() { return organizationId; }
        public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
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
