package com.resto.catalog.domain;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "product_modifier_groups")
@IdClass(ProductModifierGroup.PK.class)
public class ProductModifierGroup {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Id
    @Column(name = "modifier_group_id")
    private UUID modifierGroupId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public UUID getModifierGroupId() { return modifierGroupId; }
    public void setModifierGroupId(UUID modifierGroupId) { this.modifierGroupId = modifierGroupId; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public static class PK implements Serializable {
        private UUID productId;
        private UUID modifierGroupId;

        public PK() {}
        public PK(UUID productId, UUID modifierGroupId) {
            this.productId = productId;
            this.modifierGroupId = modifierGroupId;
        }
        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        public UUID getModifierGroupId() { return modifierGroupId; }
        public void setModifierGroupId(UUID modifierGroupId) { this.modifierGroupId = modifierGroupId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(productId, pk.productId) && Objects.equals(modifierGroupId, pk.modifierGroupId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productId, modifierGroupId);
        }
    }
}
