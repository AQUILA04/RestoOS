package com.resto.tenant.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String timezone = "UTC";

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "EUR";

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
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

    public static StoreBuilder builder() {
        return new StoreBuilder();
    }

    public static class StoreBuilder {
        private UUID id;
        private UUID organizationId;
        private String name;
        private String code;
        private String timezone = "UTC";
        private String currency = "EUR";
        private Boolean active = true;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public StoreBuilder id(UUID id) { this.id = id; return this; }
        public StoreBuilder organizationId(UUID organizationId) { this.organizationId = organizationId; return this; }
        public StoreBuilder name(String name) { this.name = name; return this; }
        public StoreBuilder code(String code) { this.code = code; return this; }
        public StoreBuilder timezone(String timezone) { this.timezone = timezone; return this; }
        public StoreBuilder currency(String currency) { this.currency = currency; return this; }
        public StoreBuilder active(Boolean active) { this.active = active; return this; }
        public StoreBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public StoreBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Store build() {
            Store s = new Store();
            s.setId(this.id);
            s.setOrganizationId(this.organizationId);
            s.setName(this.name);
            s.setCode(this.code);
            s.setTimezone(this.timezone != null ? this.timezone : "UTC");
            s.setCurrency(this.currency != null ? this.currency : "EUR");
            s.setActive(this.active != null ? this.active : true);
            s.setCreatedAt(this.createdAt);
            s.setUpdatedAt(this.updatedAt);
            return s;
        }
    }
}
