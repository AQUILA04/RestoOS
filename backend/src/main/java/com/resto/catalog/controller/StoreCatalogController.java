package com.resto.catalog.controller;

import com.resto.catalog.domain.StoreProduct;
import com.resto.catalog.dto.ResolvedProductDto;
import com.resto.catalog.service.StoreCatalogService;
import com.resto.core.response.Response;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog/stores")
public class StoreCatalogController {

    private final StoreCatalogService storeCatalogService;

    public StoreCatalogController(StoreCatalogService storeCatalogService) {
        this.storeCatalogService = storeCatalogService;
    }

    @PostMapping("/{storeId}/overrides")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER')")
    public Response<StoreProduct> setStoreOverride(@PathVariable("storeId") UUID storeId,
                                                   @RequestBody StoreOverrideRequest request) {
        StoreProduct result = storeCatalogService.setStoreOverride(
                request.getOrganizationId(),
                storeId,
                request.getProductId(),
                request.getOverridePrice(),
                request.getAvailable()
        );
        return Response.<StoreProduct>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(result)
                .build();
    }

    @GetMapping("/{storeId}/products")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER')")
    public Response<List<ResolvedProductDto>> getResolvedCatalog(@PathVariable("storeId") UUID storeId,
                                                                  @RequestParam("organizationId") UUID organizationId) {
        List<ResolvedProductDto> products = storeCatalogService.getResolvedCatalogForStore(organizationId, storeId);
        return Response.<List<ResolvedProductDto>>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(products)
                .build();
    }

    @Data
    public static class StoreOverrideRequest {
        private UUID organizationId;
        private UUID productId;
        private BigDecimal overridePrice;
        private Boolean available;
    }
}
