package com.resto.floorplan.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "restaurant_tables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "zone_id", nullable = false)
    private UUID zoneId;

    @Column(name = "table_number", nullable = false, length = 50)
    private String tableNumber;

    @Column(nullable = false)
    @Builder.Default
    private Integer capacity = 4;

    @Column(name = "pos_x", nullable = false)
    @Builder.Default
    private Integer posX = 0;

    @Column(name = "pos_y", nullable = false)
    @Builder.Default
    private Integer posY = 0;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String shape = "SQUARE";

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "AVAILABLE"; // AVAILABLE, OCCUPIED, RESERVED, OUT_OF_SERVICE

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
