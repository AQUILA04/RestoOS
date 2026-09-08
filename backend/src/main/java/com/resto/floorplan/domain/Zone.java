package com.resto.floorplan.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "zones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(nullable = false)
    private String name;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public static ZoneBuilder builder() {
        return new ZoneBuilder();
    }

    public static class ZoneBuilder {
        private UUID id;
        private UUID organizationId;
        private UUID storeId;
        private String name;
        private Integer displayOrder = 0;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public ZoneBuilder id(UUID id) { this.id = id; return this; }
        public ZoneBuilder organizationId(UUID organizationId) { this.organizationId = organizationId; return this; }
        public ZoneBuilder storeId(UUID storeId) { this.storeId = storeId; return this; }
        public ZoneBuilder name(String name) { this.name = name; return this; }
        public ZoneBuilder displayOrder(Integer displayOrder) { this.displayOrder = displayOrder; return this; }
        public ZoneBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ZoneBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Zone build() {
            Zone zone = new Zone();
            zone.setId(this.id);
            zone.setOrganizationId(this.organizationId);
            zone.setStoreId(this.storeId);
            zone.setName(this.name);
            zone.setDisplayOrder(this.displayOrder != null ? this.displayOrder : 0);
            zone.setCreatedAt(this.createdAt);
            zone.setUpdatedAt(this.updatedAt);
            return zone;
        }
    }
}
