package com.resto.catalog.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String name;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
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

    public static CategoryBuilder builder() {
        return new CategoryBuilder();
    }

    public static class CategoryBuilder {
        private UUID id;
        private UUID organizationId;
        private String name;
        private Integer displayOrder = 0;
        private Boolean active = true;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public CategoryBuilder id(UUID id) { this.id = id; return this; }
        public CategoryBuilder organizationId(UUID organizationId) { this.organizationId = organizationId; return this; }
        public CategoryBuilder name(String name) { this.name = name; return this; }
        public CategoryBuilder displayOrder(Integer displayOrder) { this.displayOrder = displayOrder; return this; }
        public CategoryBuilder active(Boolean active) { this.active = active; return this; }
        public CategoryBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public CategoryBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Category build() {
            Category c = new Category();
            c.setId(this.id);
            c.setOrganizationId(this.organizationId);
            c.setName(this.name);
            c.setDisplayOrder(this.displayOrder != null ? this.displayOrder : 0);
            c.setActive(this.active != null ? this.active : true);
            c.setCreatedAt(this.createdAt);
            c.setUpdatedAt(this.updatedAt);
            return c;
        }
    }
}
