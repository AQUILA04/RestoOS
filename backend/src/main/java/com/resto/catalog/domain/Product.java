package com.resto.catalog.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRate = new BigDecimal("10.00");

    @Column(name = "image_url")
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "is_86", nullable = false)
    @Builder.Default
    private Boolean is86 = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Boolean getIs86() { return is86; }
    public void setIs86(Boolean is86) { this.is86 = is86; }
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

    public static ProductBuilder builder() {
        return new ProductBuilder();
    }

    public static class ProductBuilder {
        private UUID id;
        private UUID organizationId;
        private UUID categoryId;
        private String name;
        private String description;
        private BigDecimal basePrice;
        private BigDecimal taxRate = new BigDecimal("10.00");
        private String imageUrl;
        private Boolean active = true;
        private Boolean is86 = false;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public ProductBuilder id(UUID id) { this.id = id; return this; }
        public ProductBuilder organizationId(UUID organizationId) { this.organizationId = organizationId; return this; }
        public ProductBuilder categoryId(UUID categoryId) { this.categoryId = categoryId; return this; }
        public ProductBuilder name(String name) { this.name = name; return this; }
        public ProductBuilder description(String description) { this.description = description; return this; }
        public ProductBuilder basePrice(BigDecimal basePrice) { this.basePrice = basePrice; return this; }
        public ProductBuilder taxRate(BigDecimal taxRate) { this.taxRate = taxRate; return this; }
        public ProductBuilder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public ProductBuilder active(Boolean active) { this.active = active; return this; }
        public ProductBuilder is86(Boolean is86) { this.is86 = is86; return this; }
        public ProductBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ProductBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Product build() {
            Product p = new Product();
            p.setId(this.id);
            p.setOrganizationId(this.organizationId);
            p.setCategoryId(this.categoryId);
            p.setName(this.name);
            p.setDescription(this.description);
            p.setBasePrice(this.basePrice);
            p.setTaxRate(this.taxRate != null ? this.taxRate : new BigDecimal("10.00"));
            p.setImageUrl(this.imageUrl);
            p.setActive(this.active != null ? this.active : true);
            p.setIs86(this.is86 != null ? this.is86 : false);
            p.setCreatedAt(this.createdAt);
            p.setUpdatedAt(this.updatedAt);
            return p;
        }
    }
}
