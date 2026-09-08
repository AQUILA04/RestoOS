package com.resto.tenant.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "membership_stores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipStore {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "membership_id", nullable = false)
    private UUID membershipId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public UUID getMembershipId() { return membershipId; }
    public void setMembershipId(UUID membershipId) { this.membershipId = membershipId; }
    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public static MembershipStoreBuilder builder() {
        return new MembershipStoreBuilder();
    }

    public static class MembershipStoreBuilder {
        private UUID id;
        private UUID organizationId;
        private UUID membershipId;
        private UUID storeId;
        private OffsetDateTime createdAt;

        public MembershipStoreBuilder id(UUID id) { this.id = id; return this; }
        public MembershipStoreBuilder organizationId(UUID organizationId) { this.organizationId = organizationId; return this; }
        public MembershipStoreBuilder membershipId(UUID membershipId) { this.membershipId = membershipId; return this; }
        public MembershipStoreBuilder storeId(UUID storeId) { this.storeId = storeId; return this; }
        public MembershipStoreBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }

        public MembershipStore build() {
            MembershipStore ms = new MembershipStore();
            ms.setId(this.id);
            ms.setOrganizationId(this.organizationId);
            ms.setMembershipId(this.membershipId);
            ms.setStoreId(this.storeId);
            ms.setCreatedAt(this.createdAt);
            return ms;
        }
    }
}
