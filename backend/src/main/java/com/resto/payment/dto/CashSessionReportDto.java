package com.resto.payment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CashSessionReportDto {

    private UUID sessionId;
    private UUID storeId;
    private String storeName;
    private UUID cashierUserId;
    private String cashierName;
    private String status;
    private OffsetDateTime openedAt;
    private OffsetDateTime closedAt;
    private BigDecimal openingFloat;
    private String closingNotes;

    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private int paymentCount;
    private Map<String, BigDecimal> revenueByMethod = new LinkedHashMap<>();
    private List<ProductLine> products = new ArrayList<>();
    private List<OrderLine> orders = new ArrayList<>();

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }
    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public UUID getCashierUserId() { return cashierUserId; }
    public void setCashierUserId(UUID cashierUserId) { this.cashierUserId = cashierUserId; }
    public String getCashierName() { return cashierName; }
    public void setCashierName(String cashierName) { this.cashierName = cashierName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(OffsetDateTime openedAt) { this.openedAt = openedAt; }
    public OffsetDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(OffsetDateTime closedAt) { this.closedAt = closedAt; }
    public BigDecimal getOpeningFloat() { return openingFloat; }
    public void setOpeningFloat(BigDecimal openingFloat) { this.openingFloat = openingFloat; }
    public String getClosingNotes() { return closingNotes; }
    public void setClosingNotes(String closingNotes) { this.closingNotes = closingNotes; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    public int getPaymentCount() { return paymentCount; }
    public void setPaymentCount(int paymentCount) { this.paymentCount = paymentCount; }
    public Map<String, BigDecimal> getRevenueByMethod() { return revenueByMethod; }
    public void setRevenueByMethod(Map<String, BigDecimal> revenueByMethod) { this.revenueByMethod = revenueByMethod; }
    public List<ProductLine> getProducts() { return products; }
    public void setProducts(List<ProductLine> products) { this.products = products; }
    public List<OrderLine> getOrders() { return orders; }
    public void setOrders(List<OrderLine> orders) { this.orders = orders; }

    public static class ProductLine {
        private UUID productId;
        private String productName;
        private int quantity;
        private BigDecimal totalAmount = BigDecimal.ZERO;

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    }

    public static class OrderLine {
        private UUID orderId;
        private Integer orderNumber;
        private OffsetDateTime createdAt;
        private String orderType;
        private String status;
        private String paymentStatus;
        private BigDecimal orderTotal;
        private BigDecimal sessionPaidAmount;
        private List<String> paymentMethods = new ArrayList<>();
        private UUID createdByUserId;
        private String createdByName;

        public UUID getOrderId() { return orderId; }
        public void setOrderId(UUID orderId) { this.orderId = orderId; }
        public Integer getOrderNumber() { return orderNumber; }
        public void setOrderNumber(Integer orderNumber) { this.orderNumber = orderNumber; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
        public String getOrderType() { return orderType; }
        public void setOrderType(String orderType) { this.orderType = orderType; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getPaymentStatus() { return paymentStatus; }
        public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
        public BigDecimal getOrderTotal() { return orderTotal; }
        public void setOrderTotal(BigDecimal orderTotal) { this.orderTotal = orderTotal; }
        public BigDecimal getSessionPaidAmount() { return sessionPaidAmount; }
        public void setSessionPaidAmount(BigDecimal sessionPaidAmount) { this.sessionPaidAmount = sessionPaidAmount; }
        public List<String> getPaymentMethods() { return paymentMethods; }
        public void setPaymentMethods(List<String> paymentMethods) { this.paymentMethods = paymentMethods; }
        public UUID getCreatedByUserId() { return createdByUserId; }
        public void setCreatedByUserId(UUID createdByUserId) { this.createdByUserId = createdByUserId; }
        public String getCreatedByName() { return createdByName; }
        public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    }
}
