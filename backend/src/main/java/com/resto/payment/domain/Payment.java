package com.resto.payment.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "cashier_user_id")
    private UUID cashierUserId;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod; // CASH, CARD, MOBILE_MONEY, OTHER

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public UUID getCashierUserId() { return cashierUserId; }
    public void setCashierUserId(UUID cashierUserId) { this.cashierUserId = cashierUserId; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public static PaymentBuilder builder() {
        return new PaymentBuilder();
    }

    public static class PaymentBuilder {
        private UUID id;
        private UUID organizationId;
        private UUID storeId;
        private UUID orderId;
        private UUID cashierUserId;
        private String paymentMethod;
        private BigDecimal amount;
        private OffsetDateTime createdAt;

        public PaymentBuilder id(UUID id) { this.id = id; return this; }
        public PaymentBuilder organizationId(UUID organizationId) { this.organizationId = organizationId; return this; }
        public PaymentBuilder storeId(UUID storeId) { this.storeId = storeId; return this; }
        public PaymentBuilder orderId(UUID orderId) { this.orderId = orderId; return this; }
        public PaymentBuilder cashierUserId(UUID cashierUserId) { this.cashierUserId = cashierUserId; return this; }
        public PaymentBuilder paymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; return this; }
        public PaymentBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public PaymentBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Payment build() {
            Payment p = new Payment();
            p.setId(this.id);
            p.setOrganizationId(this.organizationId);
            p.setStoreId(this.storeId);
            p.setOrderId(this.orderId);
            p.setCashierUserId(this.cashierUserId);
            p.setPaymentMethod(this.paymentMethod);
            p.setAmount(this.amount);
            p.setCreatedAt(this.createdAt);
            return p;
        }
    }
}
