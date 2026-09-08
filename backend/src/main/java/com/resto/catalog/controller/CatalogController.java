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

    @Data
    public static class CreateCategoryRequest {
        private UUID organizationId;
        private String name;
        private Integer displayOrder;
    }

    @Data
    public static class CreateProductRequest {
        private UUID organizationId;
        private UUID categoryId;
        private String name;
        private String description;
        private BigDecimal basePrice;
        private BigDecimal taxRate;
        private String imageUrl;
    }

    @Data
    public static class CreateModifierGroupRequest {
        private UUID organizationId;
        private String name;
        private Integer minSelection;
        private Integer maxSelection;
        private Boolean required;
    }

    @Data
    public static class AddModifierOptionRequest {
        private UUID organizationId;
        private UUID modifierGroupId;
        private String name;
        private BigDecimal priceDelta;
        private Integer displayOrder;
    }
}
