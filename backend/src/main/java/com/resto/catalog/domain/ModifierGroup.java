package com.resto.catalog.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "modifier_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifierGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String name;

    @Column(name = "min_selection", nullable = false)
    @Builder.Default
    private Integer minSelection = 0;

    @Column(name = "max_selection", nullable = false)
    @Builder.Default
    private Integer maxSelection = 1;

    @Column(nullable = false)
    @Builder.Default
    private Boolean required = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getMinSelection() { return minSelection; }
    public void setMinSelection(Integer minSelection) { this.minSelection = minSelection; }
    public Integer getMaxSelection() { return maxSelection; }
    public void setMaxSelection(Integer maxSelection) { this.maxSelection = maxSelection; }
    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public static ModifierGroupBuilder builder() {
        return new ModifierGroupBuilder();
    }

    public static class ModifierGroupBuilder {
        private UUID id;
        private UUID organizationId;
        private String name;
        private Integer minSelection = 0;
        private Integer maxSelection = 1;
        private Boolean required = false;
        private OffsetDateTime createdAt;

        public ModifierGroupBuilder id(UUID id) { this.id = id; return this; }
        public ModifierGroupBuilder organizationId(UUID organizationId) { this.organizationId = organizationId; return this; }
        public ModifierGroupBuilder name(String name) { this.name = name; return this; }
        public ModifierGroupBuilder minSelection(Integer minSelection) { this.minSelection = minSelection; return this; }
        public ModifierGroupBuilder maxSelection(Integer maxSelection) { this.maxSelection = maxSelection; return this; }
        public ModifierGroupBuilder required(Boolean required) { this.required = required; return this; }
        public ModifierGroupBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ModifierGroup build() {
            ModifierGroup mg = new ModifierGroup();
            mg.setId(this.id);
            mg.setOrganizationId(this.organizationId);
            mg.setName(this.name);
            mg.setMinSelection(this.minSelection != null ? this.minSelection : 0);
            mg.setMaxSelection(this.maxSelection != null ? this.maxSelection : 1);
            mg.setRequired(this.required != null ? this.required : false);
            mg.setCreatedAt(this.createdAt);
            return mg;
        }
    }
}
