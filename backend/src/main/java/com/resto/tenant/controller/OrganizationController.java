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
        Organization org = tenantService.createOrganization(request.getName(), request.getCode());
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

    public static class CreateOrgRequest {
        private String name;
        private String code;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
}
