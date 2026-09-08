package com.resto.tenant.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "memberships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public static MembershipBuilder builder() {
        return new MembershipBuilder();
    }

    public static class MembershipBuilder {
        private UUID id;
        private UUID organizationId;
        private UUID userId;
        private String role;
        private OffsetDateTime createdAt;

        public MembershipBuilder id(UUID id) { this.id = id; return this; }
        public MembershipBuilder organizationId(UUID organizationId) { this.organizationId = organizationId; return this; }
        public MembershipBuilder userId(UUID userId) { this.userId = userId; return this; }
        public MembershipBuilder role(String role) { this.role = role; return this; }
        public MembershipBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Membership build() {
            Membership m = new Membership();
            m.setId(this.id);
            m.setOrganizationId(this.organizationId);
            m.setUserId(this.userId);
            m.setRole(this.role);
            m.setCreatedAt(this.createdAt);
            return m;
        }
    }
}
