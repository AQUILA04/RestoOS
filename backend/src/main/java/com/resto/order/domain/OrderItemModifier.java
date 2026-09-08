package com.resto.order.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_item_modifiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemModifier {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(name = "modifier_option_id", nullable = false)
    private UUID modifierOptionId;

    @Column(name = "modifier_name", nullable = false)
    private String modifierName;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public OrderItem getOrderItem() { return orderItem; }
    public void setOrderItem(OrderItem orderItem) { this.orderItem = orderItem; }
    public UUID getModifierOptionId() { return modifierOptionId; }
    public void setModifierOptionId(UUID modifierOptionId) { this.modifierOptionId = modifierOptionId; }
    public String getModifierName() { return modifierName; }
    public void setModifierName(String modifierName) { this.modifierName = modifierName; }
    @Column(name = "price_delta", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceDelta = BigDecimal.ZERO;

    public BigDecimal getPriceDelta() { return priceDelta; }
    public void setPriceDelta(BigDecimal priceDelta) { this.priceDelta = priceDelta; }

    public static OrderItemModifierBuilder builder() {
        return new OrderItemModifierBuilder();
    }

    public static class OrderItemModifierBuilder {
        private UUID id;
        private UUID organizationId;
        private OrderItem orderItem;
        private UUID modifierOptionId;
        private String modifierName;
        private BigDecimal priceDelta = BigDecimal.ZERO;

        public OrderItemModifierBuilder id(UUID id) { this.id = id; return this; }
        public OrderItemModifierBuilder organizationId(UUID organizationId) { this.organizationId = organizationId; return this; }
        public OrderItemModifierBuilder orderItem(OrderItem orderItem) { this.orderItem = orderItem; return this; }
        public OrderItemModifierBuilder modifierOptionId(UUID modifierOptionId) { this.modifierOptionId = modifierOptionId; return this; }
        public OrderItemModifierBuilder modifierName(String modifierName) { this.modifierName = modifierName; return this; }
        public OrderItemModifierBuilder priceDelta(BigDecimal priceDelta) { this.priceDelta = priceDelta; return this; }

        public OrderItemModifier build() {
            OrderItemModifier mod = new OrderItemModifier();
            mod.setId(this.id);
            mod.setOrganizationId(this.organizationId);
            mod.setOrderItem(this.orderItem);
            mod.setModifierOptionId(this.modifierOptionId);
            mod.setModifierName(this.modifierName);
            mod.setPriceDelta(this.priceDelta != null ? this.priceDelta : BigDecimal.ZERO);
            return mod;
        }
    }
}
