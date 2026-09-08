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

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }
    public UUID getZoneId() { return zoneId; }
    public void setZoneId(UUID zoneId) { this.zoneId = zoneId; }
    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String tableNumber) { this.tableNumber = tableNumber; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public Integer getPosX() { return posX; }
    public void setPosX(Integer posX) { this.posX = posX; }
    public Integer getPosY() { return posY; }
    public void setPosY(Integer posY) { this.posY = posY; }
    public String getShape() { return shape; }
    public void setShape(String shape) { this.shape = shape; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
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

    public static RestaurantTableBuilder builder() {
        return new RestaurantTableBuilder();
    }

    public static class RestaurantTableBuilder {
        private UUID id;
        private UUID organizationId;
        private UUID storeId;
        private UUID zoneId;
        private String tableNumber;
        private Integer capacity = 4;
        private Integer posX = 0;
        private Integer posY = 0;
        private String shape = "SQUARE";
        private String status = "AVAILABLE";
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public RestaurantTableBuilder id(UUID id) { this.id = id; return this; }
        public RestaurantTableBuilder organizationId(UUID organizationId) { this.organizationId = organizationId; return this; }
        public RestaurantTableBuilder storeId(UUID storeId) { this.storeId = storeId; return this; }
        public RestaurantTableBuilder zoneId(UUID zoneId) { this.zoneId = zoneId; return this; }
        public RestaurantTableBuilder tableNumber(String tableNumber) { this.tableNumber = tableNumber; return this; }
        public RestaurantTableBuilder capacity(Integer capacity) { this.capacity = capacity; return this; }
        public RestaurantTableBuilder posX(Integer posX) { this.posX = posX; return this; }
        public RestaurantTableBuilder posY(Integer posY) { this.posY = posY; return this; }
        public RestaurantTableBuilder shape(String shape) { this.shape = shape; return this; }
        public RestaurantTableBuilder status(String status) { this.status = status; return this; }
        public RestaurantTableBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public RestaurantTableBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public RestaurantTable build() {
            RestaurantTable table = new RestaurantTable();
            table.setId(this.id);
            table.setOrganizationId(this.organizationId);
            table.setStoreId(this.storeId);
            table.setZoneId(this.zoneId);
            table.setTableNumber(this.tableNumber);
            table.setCapacity(this.capacity != null ? this.capacity : 4);
            table.setPosX(this.posX != null ? this.posX : 0);
            table.setPosY(this.posY != null ? this.posY : 0);
            table.setShape(this.shape != null ? this.shape : "SQUARE");
            table.setStatus(this.status != null ? this.status : "AVAILABLE");
            table.setCreatedAt(this.createdAt);
            table.setUpdatedAt(this.updatedAt);
            return table;
        }
    }
}
