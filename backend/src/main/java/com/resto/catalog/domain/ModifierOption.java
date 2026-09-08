package com.resto.catalog.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "modifier_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifierOption {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "modifier_group_id", nullable = false)
    private UUID modifierGroupId;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_delta", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal priceDelta = BigDecimal.ZERO;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public UUID getModifierGroupId() { return modifierGroupId; }
    public void setModifierGroupId(UUID modifierGroupId) { this.modifierGroupId = modifierGroupId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPriceDelta() { return priceDelta; }
    public void setPriceDelta(BigDecimal priceDelta) { this.priceDelta = priceDelta; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public static ModifierOptionBuilder builder() {
        return new ModifierOptionBuilder();
    }

    public static class ModifierOptionBuilder {
        private UUID id;
        private UUID organizationId;
        private UUID modifierGroupId;
        private String name;
        private BigDecimal priceDelta = BigDecimal.ZERO;
        private Integer displayOrder = 0;
        private OffsetDateTime createdAt;

        public ModifierOptionBuilder id(UUID id) { this.id = id; return this; }
        public ModifierOptionBuilder organizationId(UUID organizationId) { this.organizationId = organizationId; return this; }
        public ModifierOptionBuilder modifierGroupId(UUID modifierGroupId) { this.modifierGroupId = modifierGroupId; return this; }
        public ModifierOptionBuilder name(String name) { this.name = name; return this; }
        public ModifierOptionBuilder priceDelta(BigDecimal priceDelta) { this.priceDelta = priceDelta; return this; }
        public ModifierOptionBuilder displayOrder(Integer displayOrder) { this.displayOrder = displayOrder; return this; }
        public ModifierOptionBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ModifierOption build() {
            ModifierOption mo = new ModifierOption();
            mo.setId(this.id);
            mo.setOrganizationId(this.organizationId);
            mo.setModifierGroupId(this.modifierGroupId);
            mo.setName(this.name);
            mo.setPriceDelta(this.priceDelta != null ? this.priceDelta : BigDecimal.ZERO);
            mo.setDisplayOrder(this.displayOrder != null ? this.displayOrder : 0);
            mo.setCreatedAt(this.createdAt);
            return mo;
        }
    }
}
