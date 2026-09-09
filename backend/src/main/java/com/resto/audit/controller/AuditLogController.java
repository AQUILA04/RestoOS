package com.resto.audit.controller;

import com.resto.audit.domain.AuditLog;
import com.resto.audit.repository.AuditLogRepository;
import com.resto.core.response.Response;
import com.resto.core.security.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER')")
    public Response<List<AuditLog>> list(
            @RequestParam(value = "storeId", required = false) UUID storeId) {
        UUID orgId = TenantContext.getOrgId();
        List<AuditLog> logs;
        if (storeId != null) {
            logs = auditLogRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
        } else if (orgId != null) {
            logs = auditLogRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
        } else {
            logs = auditLogRepository.findAll();
        }
        return Response.<List<AuditLog>>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(logs)
                .build();
    }
}
