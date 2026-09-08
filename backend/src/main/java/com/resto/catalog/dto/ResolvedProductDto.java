package com.resto.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
