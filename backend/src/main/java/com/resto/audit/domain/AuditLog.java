package com.resto.audit.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public static AuditLogBuilder builder() {
        return new AuditLogBuilder();
    }

    public static class AuditLogBuilder {
        private UUID id;
        private UUID organizationId;
        private UUID storeId;
        private UUID userId;
        private String action;
        private String entityType;
        private UUID entityId;
        private String details;
        private OffsetDateTime createdAt;

        public AuditLogBuilder id(UUID id) { this.id = id; return this; }
        public AuditLogBuilder organizationId(UUID organizationId) { this.organizationId = organizationId; return this; }
        public AuditLogBuilder storeId(UUID storeId) { this.storeId = storeId; return this; }
        public AuditLogBuilder userId(UUID userId) { this.userId = userId; return this; }
        public AuditLogBuilder action(String action) { this.action = action; return this; }
        public AuditLogBuilder entityType(String entityType) { this.entityType = entityType; return this; }
        public AuditLogBuilder entityId(UUID entityId) { this.entityId = entityId; return this; }
        public AuditLogBuilder details(String details) { this.details = details; return this; }
        public AuditLogBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }

        public AuditLog build() {
            AuditLog al = new AuditLog();
            al.setId(this.id);
            al.setOrganizationId(this.organizationId);
            al.setStoreId(this.storeId);
            al.setUserId(this.userId);
            al.setAction(this.action);
            al.setEntityType(this.entityType);
            al.setEntityId(this.entityId);
            al.setDetails(this.details);
            al.setCreatedAt(this.createdAt);
            return al;
        }
    }
}
