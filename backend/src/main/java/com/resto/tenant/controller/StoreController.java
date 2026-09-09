package com.resto.tenant.controller;

import com.resto.core.response.Response;
import com.resto.core.security.JwtAuth;
import com.resto.core.security.TenantContext;
import com.resto.tenant.domain.Store;
import com.resto.tenant.service.TenantService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stores")
public class StoreController {

    private final TenantService tenantService;

    public StoreController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<Store> createStore(@RequestBody CreateStoreRequest request) {
        UUID organizationId = TenantContext.getOrgId() != null
                ? TenantContext.getOrgId()
                : JwtAuth.organizationId();

        String code = (request.getCode() != null && !request.getCode().isBlank())
                ? request.getCode()
                : request.getName().toLowerCase()
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("^-|-$", "")
                        .substring(0, Math.min(Math.max(request.getName().length(), 1), 48))
                + "-" + System.currentTimeMillis() % 10000;

        Store store = tenantService.createStore(
                organizationId,
                request.getName(),
                code,
                request.getTimezone() != null ? request.getTimezone() : "UTC",
                request.getCurrency() != null ? request.getCurrency() : "EUR"
        );
        return Response.<Store>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(store)
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER')")
    public Response<List<Store>> getStoresByOrg(
            @RequestParam(value = "organizationId", required = false) UUID organizationId) {
        UUID orgId = organizationId != null ? organizationId
                : (TenantContext.getOrgId() != null ? TenantContext.getOrgId() : JwtAuth.organizationId());
        List<Store> stores = tenantService.getStoresByOrganization(orgId);
        return Response.<List<Store>>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(stores)
                .build();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<Store> updateStore(@PathVariable("id") UUID id, @RequestBody UpdateStoreRequest request) {
        Store store = tenantService.updateStoreName(id, request.getName());
        return Response.<Store>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(store)
                .build();
    }

    public static class CreateStoreRequest {
        private UUID organizationId;
        private String name;
        private String code;
        private String timezone;
        private String currency;
        private String city;

        public UUID getOrganizationId() { return organizationId; }
        public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
    }

    public static class UpdateStoreRequest {
        private String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
