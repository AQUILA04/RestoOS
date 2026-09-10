package com.resto.tenant.controller;

import com.resto.core.response.Response;
import com.resto.tenant.domain.Organization;
import com.resto.tenant.service.TenantService;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final TenantService tenantService;

    public OrganizationController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<Organization> createOrganization(@RequestBody CreateOrgRequest request) {
        // Derive a URL-safe code from the name when the caller omits it
        String code = (request.getCode() != null && !request.getCode().isBlank())
                ? request.getCode()
                : request.getName().toLowerCase()
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("^-|-$", "")
                        .substring(0, Math.min(request.getName().length(), 48))
                + "-" + System.currentTimeMillis() % 10000;

        Organization org = tenantService.createOrganization(request.getName(), code);
        return Response.<Organization>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(org)
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<List<Organization>> getAllOrganizations() {
        List<Organization> orgs = tenantService.getAllOrganizations();
        return Response.<List<Organization>>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(orgs)
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER', 'KITCHEN')")
    public Response<Organization> getOrganization(@PathVariable("id") java.util.UUID id) {
        Organization org = tenantService.getOrganizationById(id);
        return Response.<Organization>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(org)
                .build();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER')")
    public Response<Organization> updateOrganization(
            @PathVariable("id") java.util.UUID id,
            @RequestBody UpdateOrgRequest request) {
        Organization org = tenantService.updateOrganizationSettings(
                id, request.getName(), request.getMobileMoneyLabel(), request.getLogoUrl());
        return Response.<Organization>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(org)
                .build();
    }

    public static class CreateOrgRequest {
        private String name;
        private String code;
        private String country;   // accepted from E2E payload, stored for future use
        private String currency;  // accepted from E2E payload, stored for future use

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }

    public static class UpdateOrgRequest {
        private String name;
        private String mobileMoneyLabel;
        private String logoUrl;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getMobileMoneyLabel() { return mobileMoneyLabel; }
        public void setMobileMoneyLabel(String mobileMoneyLabel) { this.mobileMoneyLabel = mobileMoneyLabel; }
        public String getLogoUrl() { return logoUrl; }
        public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    }
}
