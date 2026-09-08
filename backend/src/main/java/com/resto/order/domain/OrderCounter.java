package com.resto.order.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_counters")
public class OrderCounter {

    @Id
    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "last_order_number", nullable = false)
    private Integer lastOrderNumber = 100;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public Integer getLastOrderNumber() { return lastOrderNumber; }
    public void setLastOrderNumber(Integer lastOrderNumber) { this.lastOrderNumber = lastOrderNumber; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = OffsetDateTime.now();
    }
}
