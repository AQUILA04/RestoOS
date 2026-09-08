package com.resto.catalog.controller;

import com.resto.catalog.domain.StoreProduct;
import com.resto.catalog.service.StoreCatalogService;
import com.resto.core.response.Response;
import com.resto.core.security.JwtAuth;
import com.resto.core.security.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Legacy 86 endpoint — delegates to store-local availability.
 */
@RestController
@RequestMapping("/api/v1/catalog")
public class Stock86Controller {

    private final StoreCatalogService storeCatalogService;

    public Stock86Controller(StoreCatalogService storeCatalogService) {
        this.storeCatalogService = storeCatalogService;
    }

    @PostMapping("/products/{productId}/86")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER')")
    public Response<StoreProduct> toggleStock86(@PathVariable("productId") UUID productId,
                                                @RequestBody Toggle86Request request) {
        if (request.getStoreId() == null) {
            throw new IllegalArgumentException("storeId is required for store-local 86");
        }
        UUID organizationId = TenantContext.getOrgId() != null ? TenantContext.getOrgId() : JwtAuth.organizationId();
        boolean available = !Boolean.TRUE.equals(request.getIs86());
        StoreProduct updated = storeCatalogService.setAvailability(
                organizationId, request.getStoreId(), productId, available
        );
        return Response.<StoreProduct>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(updated)
                .build();
    }

    public static class Toggle86Request {
        private UUID storeId;
        private Boolean is86;
        public UUID getStoreId() { return storeId; }
        public void setStoreId(UUID storeId) { this.storeId = storeId; }
        public Boolean getIs86() { return is86; }
        public void setIs86(Boolean is86) { this.is86 = is86; }
    }
}
