package com.resto.audit.service;

import com.resto.audit.domain.AuditLog;
import com.resto.audit.repository.AuditLogRepository;
import com.resto.core.security.JwtAuth;
import com.resto.core.security.TenantContext;
import com.resto.tenant.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Append-only audit logging for critical domain actions.
 * Physical deletes of audit_logs must never be performed at app level.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AuditLog record(UUID organizationId, UUID storeId, UUID userId,
                           String action, String entityType, UUID entityId, String details) {
        UUID safeUserId = userId;
        if (safeUserId != null && !userRepository.existsById(safeUserId)) {
            // HMAC/station tokens may carry a synthetic subject; keep the trail without breaking FK.
            safeUserId = null;
        }
        AuditLog log = AuditLog.builder()
                .organizationId(organizationId)
                .storeId(storeId)
                .userId(safeUserId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build();
        return auditLogRepository.save(log);
    }

    @Transactional
    public AuditLog recordFromAuth(String action, String entityType, UUID entityId, String details) {
        UUID orgId = TenantContext.getOrgId() != null ? TenantContext.getOrgId() : JwtAuth.organizationId();
        UUID storeId = TenantContext.getStoreId();
        UUID userId = TenantContext.getUserId() != null ? TenantContext.getUserId() : safeUserId();
        return record(orgId, storeId, userId, action, entityType, entityId, details);
    }

    private UUID safeUserId() {
        try {
            return JwtAuth.userId();
        } catch (Exception e) {
            return null;
        }
    }
}
