package com.resto.order.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "table_id")
    private UUID tableId;

    @Column(name = "order_number", nullable = false)
    private Integer orderNumber;

    @Column(name = "order_type", nullable = false, length = 50)
    private String orderType = "DINE_IN";

    @Column(nullable = false, length = 50)
    private String status = "CREATED";

    @Column(name = "payment_status", nullable = false, length = 50)
    private String paymentStatus = "UNPAID";

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxTotal = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column
    private String notes;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

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
    public UUID getTableId() { return tableId; }
    public void setTableId(UUID tableId) { this.tableId = tableId; }
    public Integer getOrderNumber() { return orderNumber; }
    public void setOrderNumber(Integer orderNumber) { this.orderNumber = orderNumber; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getTaxTotal() { return taxTotal; }
    public void setTaxTotal(BigDecimal taxTotal) { this.taxTotal = taxTotal; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public OffsetDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(OffsetDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public UUID getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(UUID cancelledBy) { this.cancelledBy = cancelledBy; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
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

    public static OrderBuilder builder() {
        return new OrderBuilder();
    }

    public static class OrderBuilder {
        private UUID id;
        private UUID organizationId;
        private UUID storeId;
        private UUID tableId;
        private Integer orderNumber;
        private String orderType = "DINE_IN";
        private String status = "CREATED";
        private String paymentStatus = "UNPAID";
        private BigDecimal subtotal = BigDecimal.ZERO;
        private BigDecimal taxTotal = BigDecimal.ZERO;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private String notes;
        private OffsetDateTime cancelledAt;
        private UUID cancelledBy;
        private String cancellationReason;
        private Long version;
        private List<OrderItem> items = new ArrayList<>();
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public OrderBuilder id(UUID id) { this.id = id; return this; }
        public OrderBuilder organizationId(UUID organizationId) { this.organizationId = organizationId; return this; }
        public OrderBuilder storeId(UUID storeId) { this.storeId = storeId; return this; }
        public OrderBuilder tableId(UUID tableId) { this.tableId = tableId; return this; }
        public OrderBuilder orderNumber(Integer orderNumber) { this.orderNumber = orderNumber; return this; }
        public OrderBuilder orderType(String orderType) { this.orderType = orderType; return this; }
        public OrderBuilder status(String status) { this.status = status; return this; }
        public OrderBuilder paymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; return this; }
        public OrderBuilder subtotal(BigDecimal subtotal) { this.subtotal = subtotal; return this; }
        public OrderBuilder taxTotal(BigDecimal taxTotal) { this.taxTotal = taxTotal; return this; }
        public OrderBuilder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
        public OrderBuilder notes(String notes) { this.notes = notes; return this; }
        public OrderBuilder cancelledAt(OffsetDateTime cancelledAt) { this.cancelledAt = cancelledAt; return this; }
        public OrderBuilder cancelledBy(UUID cancelledBy) { this.cancelledBy = cancelledBy; return this; }
        public OrderBuilder cancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; return this; }
        public OrderBuilder version(Long version) { this.version = version; return this; }
        public OrderBuilder items(List<OrderItem> items) { this.items = items; return this; }
        public OrderBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public OrderBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Order build() {
            Order order = new Order();
            order.setId(this.id);
            order.setOrganizationId(this.organizationId);
            order.setStoreId(this.storeId);
            order.setTableId(this.tableId);
            order.setOrderNumber(this.orderNumber);
            order.setOrderType(this.orderType != null ? this.orderType : "DINE_IN");
            order.setStatus(this.status != null ? this.status : "CREATED");
            order.setPaymentStatus(this.paymentStatus != null ? this.paymentStatus : "UNPAID");
            order.setSubtotal(this.subtotal != null ? this.subtotal : BigDecimal.ZERO);
            order.setTaxTotal(this.taxTotal != null ? this.taxTotal : BigDecimal.ZERO);
            order.setTotalAmount(this.totalAmount != null ? this.totalAmount : BigDecimal.ZERO);
            order.setNotes(this.notes);
            order.setCancelledAt(this.cancelledAt);
            order.setCancelledBy(this.cancelledBy);
            order.setCancellationReason(this.cancellationReason);
            order.setVersion(this.version);
            order.setItems(this.items != null ? this.items : new ArrayList<>());
            order.setCreatedAt(this.createdAt);
            order.setUpdatedAt(this.updatedAt);
            return order;
        }
    }
}
