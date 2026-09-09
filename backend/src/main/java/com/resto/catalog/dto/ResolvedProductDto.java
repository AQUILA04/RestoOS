package com.resto.catalog.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ResolvedProductDto {
    private UUID productId;
    private UUID categoryId;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private BigDecimal resolvedPrice;
    private BigDecimal taxRate;
    private String imageUrl;
    private Boolean is86;
    private Boolean available;
    private List<ResolvedModifierGroupDto> modifierGroups = new ArrayList<>();

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public BigDecimal getResolvedPrice() { return resolvedPrice; }
    public void setResolvedPrice(BigDecimal resolvedPrice) { this.resolvedPrice = resolvedPrice; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Boolean getIs86() { return is86; }
    public void setIs86(Boolean is86) { this.is86 = is86; }
    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
    public List<ResolvedModifierGroupDto> getModifierGroups() { return modifierGroups; }
    public void setModifierGroups(List<ResolvedModifierGroupDto> modifierGroups) {
        this.modifierGroups = modifierGroups != null ? modifierGroups : new ArrayList<>();
    }

    public static ResolvedProductDtoBuilder builder() { return new ResolvedProductDtoBuilder(); }

    public static class ResolvedProductDtoBuilder {
        private UUID productId;
        private UUID categoryId;
        private String name;
        private String description;
        private BigDecimal basePrice;
        private BigDecimal resolvedPrice;
        private BigDecimal taxRate;
        private String imageUrl;
        private Boolean is86;
        private Boolean available;
        private List<ResolvedModifierGroupDto> modifierGroups = new ArrayList<>();

        public ResolvedProductDtoBuilder productId(UUID productId) { this.productId = productId; return this; }
        public ResolvedProductDtoBuilder categoryId(UUID categoryId) { this.categoryId = categoryId; return this; }
        public ResolvedProductDtoBuilder name(String name) { this.name = name; return this; }
        public ResolvedProductDtoBuilder description(String description) { this.description = description; return this; }
        public ResolvedProductDtoBuilder basePrice(BigDecimal basePrice) { this.basePrice = basePrice; return this; }
        public ResolvedProductDtoBuilder resolvedPrice(BigDecimal resolvedPrice) { this.resolvedPrice = resolvedPrice; return this; }
        public ResolvedProductDtoBuilder taxRate(BigDecimal taxRate) { this.taxRate = taxRate; return this; }
        public ResolvedProductDtoBuilder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public ResolvedProductDtoBuilder is86(Boolean is86) { this.is86 = is86; return this; }
        public ResolvedProductDtoBuilder available(Boolean available) { this.available = available; return this; }
        public ResolvedProductDtoBuilder modifierGroups(List<ResolvedModifierGroupDto> modifierGroups) {
            this.modifierGroups = modifierGroups != null ? modifierGroups : new ArrayList<>();
            return this;
        }

        public ResolvedProductDto build() {
            ResolvedProductDto dto = new ResolvedProductDto();
            dto.setProductId(this.productId);
            dto.setCategoryId(this.categoryId);
            dto.setName(this.name);
            dto.setDescription(this.description);
            dto.setBasePrice(this.basePrice);
            dto.setResolvedPrice(this.resolvedPrice);
            dto.setTaxRate(this.taxRate);
            dto.setImageUrl(this.imageUrl);
            dto.setIs86(this.is86);
            dto.setAvailable(this.available);
            dto.setModifierGroups(this.modifierGroups);
            return dto;
        }
    }
}
