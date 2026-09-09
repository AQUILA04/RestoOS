package com.resto.catalog.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class ResolvedModifierOptionDto {
    private UUID id;
    private String name;
    private BigDecimal priceDelta;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPriceDelta() { return priceDelta; }
    public void setPriceDelta(BigDecimal priceDelta) { this.priceDelta = priceDelta; }

    public static ResolvedModifierOptionDtoBuilder builder() {
        return new ResolvedModifierOptionDtoBuilder();
    }

    public static class ResolvedModifierOptionDtoBuilder {
        private UUID id;
        private String name;
        private BigDecimal priceDelta;

        public ResolvedModifierOptionDtoBuilder id(UUID id) { this.id = id; return this; }
        public ResolvedModifierOptionDtoBuilder name(String name) { this.name = name; return this; }
        public ResolvedModifierOptionDtoBuilder priceDelta(BigDecimal priceDelta) { this.priceDelta = priceDelta; return this; }

        public ResolvedModifierOptionDto build() {
            ResolvedModifierOptionDto dto = new ResolvedModifierOptionDto();
            dto.setId(this.id);
            dto.setName(this.name);
            dto.setPriceDelta(this.priceDelta);
            return dto;
        }
    }
}
