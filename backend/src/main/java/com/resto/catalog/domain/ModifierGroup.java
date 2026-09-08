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

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
