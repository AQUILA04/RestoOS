package com.resto.catalog.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "store_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "override_price", precision = 10, scale = 2)
    private BigDecimal overridePrice;

    @Column(nullable = false)
    @Builder.Default
    private Boolean available = true;

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
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public BigDecimal getOverridePrice() { return overridePrice; }
    public void setOverridePrice(BigDecimal overridePrice) { this.overridePrice = overridePrice; }
    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
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

    public static StoreProductBuilder builder() {
        return new StoreProductBuilder();
    }

    public static class StoreProductBuilder {
        private UUID id;
        private UUID organizationId;
        private UUID storeId;
        private UUID productId;
        private BigDecimal overridePrice;
        private Boolean available = true;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public StoreProductBuilder id(UUID id) { this.id = id; return this; }
        public StoreProductBuilder organizationId(UUID organizationId) { this.organizationId = organizationId; return this; }
        public StoreProductBuilder storeId(UUID storeId) { this.storeId = storeId; return this; }
        public StoreProductBuilder productId(UUID productId) { this.productId = productId; return this; }
        public StoreProductBuilder overridePrice(BigDecimal overridePrice) { this.overridePrice = overridePrice; return this; }
        public StoreProductBuilder available(Boolean available) { this.available = available; return this; }
        public StoreProductBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public StoreProductBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public StoreProduct build() {
            StoreProduct sp = new StoreProduct();
            sp.setId(this.id);
            sp.setOrganizationId(this.organizationId);
            sp.setStoreId(this.storeId);
            sp.setProductId(this.productId);
            sp.setOverridePrice(this.overridePrice);
            sp.setAvailable(this.available != null ? this.available : true);
            sp.setCreatedAt(this.createdAt);
            sp.setUpdatedAt(this.updatedAt);
            return sp;
        }
    }
}
