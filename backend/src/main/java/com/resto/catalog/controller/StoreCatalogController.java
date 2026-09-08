package com.resto.catalog.controller;

import com.resto.catalog.domain.StoreProduct;
import com.resto.catalog.dto.ResolvedProductDto;
import com.resto.catalog.service.StoreCatalogService;
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
public class StoreCatalogController {

    private final StoreCatalogService storeCatalogService;

    public StoreCatalogController(StoreCatalogService storeCatalogService) {
        this.storeCatalogService = storeCatalogService;
    }

    @PutMapping("/api/v1/stores/{storeId}/products/{productId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER')")
    public Response<StoreProduct> putStoreOverride(@PathVariable("storeId") UUID storeId,
                                                   @PathVariable("productId") UUID productId,
                                                   @RequestBody StoreOverrideRequest request) {
        UUID organizationId = resolveOrg(request.getOrganizationId());
        BigDecimal price = request.getPriceOverride() != null ? request.getPriceOverride() : request.getOverridePrice();
        StoreProduct result = storeCatalogService.setStoreOverride(
                organizationId, storeId, productId, price, request.getAvailable()
        );
        return ok(result);
    }

    @PostMapping("/api/v1/stores/{storeId}/products/{productId}/availability")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER')")
    public Response<StoreProduct> setAvailability(@PathVariable("storeId") UUID storeId,
                                                  @PathVariable("productId") UUID productId,
                                                  @RequestBody AvailabilityRequest request) {
        UUID organizationId = resolveOrg(request.getOrganizationId());
        Boolean available = request.getAvailable() != null ? request.getAvailable() : !Boolean.TRUE.equals(request.getIs86());
        StoreProduct result = storeCatalogService.setAvailability(organizationId, storeId, productId, available);
        return ok(result);
    }

    @GetMapping({"/api/v1/stores/{storeId}/products", "/api/v1/catalog/stores/{storeId}/products"})
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER')")
    public Response<List<ResolvedProductDto>> getResolvedCatalog(
            @PathVariable("storeId") UUID storeId,
            @RequestParam(value = "organizationId", required = false) UUID organizationId) {
        UUID orgId = organizationId != null ? organizationId : resolveOrg(null);
        return ok(storeCatalogService.getResolvedCatalogForStore(orgId, storeId));
    }

    /** Legacy override path */
    @PostMapping("/api/v1/catalog/stores/{storeId}/overrides")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER')")
    public Response<StoreProduct> setStoreOverrideLegacy(@PathVariable("storeId") UUID storeId,
                                                         @RequestBody StoreOverrideRequest request) {
        UUID organizationId = resolveOrg(request.getOrganizationId());
        return ok(storeCatalogService.setStoreOverride(
                organizationId, storeId, request.getProductId(),
                request.getOverridePrice() != null ? request.getOverridePrice() : request.getPriceOverride(),
                request.getAvailable()
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

    public static class StoreOverrideRequest {
        private UUID organizationId;
        private UUID productId;
        private BigDecimal overridePrice;
        private BigDecimal priceOverride;
        private Boolean available;
        public UUID getOrganizationId() { return organizationId; }
        public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        public BigDecimal getOverridePrice() { return overridePrice; }
        public void setOverridePrice(BigDecimal overridePrice) { this.overridePrice = overridePrice; }
        public BigDecimal getPriceOverride() { return priceOverride; }
        public void setPriceOverride(BigDecimal priceOverride) { this.priceOverride = priceOverride; }
        public Boolean getAvailable() { return available; }
        public void setAvailable(Boolean available) { this.available = available; }
    }

    public static class AvailabilityRequest {
        private UUID organizationId;
        private Boolean available;
        private Boolean is86;
        public UUID getOrganizationId() { return organizationId; }
        public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
        public Boolean getAvailable() { return available; }
        public void setAvailable(Boolean available) { this.available = available; }
        public Boolean getIs86() { return is86; }
        public void setIs86(Boolean is86) { this.is86 = is86; }
    }
}
