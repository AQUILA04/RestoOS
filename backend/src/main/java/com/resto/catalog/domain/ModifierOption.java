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

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
