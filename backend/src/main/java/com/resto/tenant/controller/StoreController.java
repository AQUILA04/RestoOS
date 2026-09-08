package com.resto.tenant.controller;

import com.resto.core.response.Response;
import com.resto.tenant.domain.Store;
import com.resto.tenant.service.TenantService;
import lombok.Data;
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
        Store store = tenantService.createStore(
                request.getOrganizationId(),
                request.getName(),
                request.getCode(),
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
    }
}
