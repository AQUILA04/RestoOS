package com.resto.tenant.controller;

import com.resto.core.response.Response;
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
    public Response<Store> createStore(
            @RequestHeader(value = "X-Tenant-ID", required = false) UUID tenantId,
            @RequestBody CreateStoreRequest request) {

        // organizationId: prefer header (E2E pattern) over body field
        UUID organizationId = tenantId != null ? tenantId : request.getOrganizationId();

        // Derive a URL-safe code from the name when the caller omits it
        String code = (request.getCode() != null && !request.getCode().isBlank())
                ? request.getCode()
                : request.getName().toLowerCase()
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("^-|-$", "")
                        .substring(0, Math.min(request.getName().length(), 48))
                + "-" + System.currentTimeMillis() % 10000;

        Store store = tenantService.createStore(
                organizationId,
                request.getName(),
                code,
                request.getTimezone(),
                request.getCurrency()
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
    public Response<List<Store>> getStoresByOrg(@RequestParam("organizationId") UUID organizationId) {
        List<Store> stores = tenantService.getStoresByOrganization(organizationId);
        return Response.<List<Store>>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(stores)
                .build();
    }

    public static class CreateStoreRequest {
        private UUID organizationId;
        private String name;
        private String code;
        private String timezone;
        private String currency;
        private String city;  // accepted from E2E payload; no column yet

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
}

