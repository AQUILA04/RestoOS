package com.resto.order.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "line_notes")
    private String lineNotes;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItemModifier> modifiers = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public String getLineNotes() { return lineNotes; }
    public void setLineNotes(String lineNotes) { this.lineNotes = lineNotes; }
    public List<OrderItemModifier> getModifiers() { return modifiers; }
    public void setModifiers(List<OrderItemModifier> modifiers) { this.modifiers = modifiers; }
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public static OrderItemBuilder builder() {
        return new OrderItemBuilder();
    }

    public static class OrderItemBuilder {
        private UUID id;
        private UUID organizationId;
        private UUID storeId;
        private Order order;
        private UUID productId;
        private String productName;
        private BigDecimal unitPrice;
        private BigDecimal taxRate;
        private Integer quantity;
        private BigDecimal subtotal;
        private String lineNotes;
        private List<OrderItemModifier> modifiers = new ArrayList<>();
        private OffsetDateTime createdAt;

        public OrderItemBuilder id(UUID id) { this.id = id; return this; }
        public OrderItemBuilder organizationId(UUID organizationId) { this.organizationId = organizationId; return this; }
        public OrderItemBuilder storeId(UUID storeId) { this.storeId = storeId; return this; }
        public OrderItemBuilder order(Order order) { this.order = order; return this; }
        public OrderItemBuilder productId(UUID productId) { this.productId = productId; return this; }
        public OrderItemBuilder productName(String productName) { this.productName = productName; return this; }
        public OrderItemBuilder unitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }
        public OrderItemBuilder taxRate(BigDecimal taxRate) { this.taxRate = taxRate; return this; }
        public OrderItemBuilder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public OrderItemBuilder subtotal(BigDecimal subtotal) { this.subtotal = subtotal; return this; }
        public OrderItemBuilder lineNotes(String lineNotes) { this.lineNotes = lineNotes; return this; }
        public OrderItemBuilder modifiers(List<OrderItemModifier> modifiers) { this.modifiers = modifiers; return this; }
        public OrderItemBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }

        public OrderItem build() {
            OrderItem item = new OrderItem();
            item.setId(this.id);
            item.setOrganizationId(this.organizationId);
            item.setStoreId(this.storeId);
            item.setOrder(this.order);
            item.setProductId(this.productId);
            item.setProductName(this.productName);
            item.setUnitPrice(this.unitPrice);
            item.setTaxRate(this.taxRate);
            item.setQuantity(this.quantity);
            item.setSubtotal(this.subtotal);
            item.setLineNotes(this.lineNotes);
            item.setModifiers(this.modifiers != null ? this.modifiers : new ArrayList<>());
            item.setCreatedAt(this.createdAt);
            return item;
        }
    }
}
