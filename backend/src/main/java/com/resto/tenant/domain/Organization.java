package com.resto.tenant.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
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

    public static OrganizationBuilder builder() {
        return new OrganizationBuilder();
    }

    public static class OrganizationBuilder {
        private UUID id;
        private String name;
        private String code;
        private Boolean active = true;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public OrganizationBuilder id(UUID id) { this.id = id; return this; }
        public OrganizationBuilder name(String name) { this.name = name; return this; }
        public OrganizationBuilder code(String code) { this.code = code; return this; }
        public OrganizationBuilder active(Boolean active) { this.active = active; return this; }
        public OrganizationBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public OrganizationBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Organization build() {
            Organization o = new Organization();
            o.setId(this.id);
            o.setName(this.name);
            o.setCode(this.code);
            o.setActive(this.active != null ? this.active : true);
            o.setCreatedAt(this.createdAt);
            o.setUpdatedAt(this.updatedAt);
            return o;
        }
    }
}
