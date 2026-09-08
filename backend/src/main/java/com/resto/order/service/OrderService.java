package com.resto.order.service;

import com.resto.audit.domain.AuditLog;
import com.resto.audit.repository.AuditLogRepository;
import com.resto.catalog.domain.ModifierOption;
import com.resto.catalog.domain.Product;
import com.resto.catalog.domain.StoreProduct;
import com.resto.catalog.repository.ModifierOptionRepository;
import com.resto.catalog.repository.ProductRepository;
import com.resto.catalog.repository.StoreProductRepository;
import com.resto.order.domain.Order;
import com.resto.order.domain.OrderItem;
import com.resto.order.domain.OrderItemModifier;
import com.resto.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StoreProductRepository storeProductRepository;
    private final ModifierOptionRepository modifierOptionRepository;
    private final AuditLogRepository auditLogRepository;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        StoreProductRepository storeProductRepository,
                        ModifierOptionRepository modifierOptionRepository,
                        AuditLogRepository auditLogRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.storeProductRepository = storeProductRepository;
        this.modifierOptionRepository = modifierOptionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public Order createOrder(UUID organizationId, UUID storeId, UUID tableId, String orderType, String notes, List<CreateOrderItemParam> itemParams) {
        Integer nextOrderNumber = orderRepository.getNextOrderNumber(storeId);

        Order order = Order.builder()
                .organizationId(organizationId)
                .storeId(storeId)
                .tableId(tableId)
                .orderNumber(nextOrderNumber)
                .orderType(orderType != null ? orderType : "DINE_IN")
                .status("CREATED")
                .paymentStatus("UNPAID")
                .notes(notes)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;

        for (CreateOrderItemParam itemParam : itemParams) {
            Product product = productRepository.findById(itemParam.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemParam.getProductId()));

            if (Boolean.TRUE.equals(product.getIs86())) {
                throw new IllegalArgumentException("Product is out of stock (86): " + product.getName());
            }

            // Resolve store price or fallback base price
            StoreProduct storeProduct = storeProductRepository.findByStoreIdAndProductId(storeId, product.getId()).orElse(null);
            BigDecimal unitPrice = (storeProduct != null && storeProduct.getOverridePrice() != null)
                    ? storeProduct.getOverridePrice()
                    : product.getBasePrice();

            BigDecimal itemSubtotal = unitPrice.multiply(new BigDecimal(itemParam.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .organizationId(organizationId)
                    .storeId(storeId)
                    .order(order)
                    .productId(product.getId())
                    .productName(product.getName())
                    .unitPrice(unitPrice)
                    .taxRate(product.getTaxRate())
                    .quantity(itemParam.getQuantity())
                    .subtotal(itemSubtotal)
                    .lineNotes(itemParam.getLineNotes())
                    .build();

            if (itemParam.getModifierOptionIds() != null) {
                for (UUID modId : itemParam.getModifierOptionIds()) {
                    ModifierOption modOption = modifierOptionRepository.findById(modId)
                            .orElseThrow(() -> new IllegalArgumentException("Modifier option not found: " + modId));

                    BigDecimal priceDelta = modOption.getPriceDelta() != null ? modOption.getPriceDelta() : BigDecimal.ZERO;

                    OrderItemModifier mod = OrderItemModifier.builder()
                            .organizationId(organizationId)
                            .orderItem(orderItem)
                            .modifierOptionId(modOption.getId())
                            .modifierName(modOption.getName())
                            .priceDelta(priceDelta)
                            .build();

                    orderItem.getModifiers().add(mod);
                    itemSubtotal = itemSubtotal.add(priceDelta.multiply(new BigDecimal(itemParam.getQuantity())));
                }
            }

            orderItem.setSubtotal(itemSubtotal);
            order.getItems().add(orderItem);

            BigDecimal itemTax = itemSubtotal.multiply(product.getTaxRate()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(itemSubtotal);
            taxTotal = taxTotal.add(itemTax);
        }

        order.setSubtotal(subtotal);
        order.setTaxTotal(taxTotal);
        order.setTotalAmount(subtotal.add(taxTotal));

        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByStore(UUID storeId) {
        return orderRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
    }

    public Order cancelOrder(UUID orderId, UUID userId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Cancellation reason is required");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        order.setStatus("CANCELLED");
        order.setCancelledAt(java.time.OffsetDateTime.now());
        order.setCancelledBy(userId);
        order.setCancellationReason(reason);

        Order savedOrder = orderRepository.save(order);

        // Audit Log Entry
        AuditLog auditLog = AuditLog.builder()
                .organizationId(order.getOrganizationId())
                .storeId(order.getStoreId())
                .userId(userId)
                .action("ORDER_CANCELLED")
                .entityType("ORDER")
                .entityId(orderId)
                .details("Order cancelled with reason: " + reason)
                .build();
        auditLogRepository.save(auditLog);

        return savedOrder;
    }

    public static class CreateOrderItemParam {
        private UUID productId;
        private Integer quantity;
        private String lineNotes;
        private List<UUID> modifierOptionIds;

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public String getLineNotes() { return lineNotes; }
        public void setLineNotes(String lineNotes) { this.lineNotes = lineNotes; }
        public List<UUID> getModifierOptionIds() { return modifierOptionIds; }
        public void setModifierOptionIds(List<UUID> modifierOptionIds) { this.modifierOptionIds = modifierOptionIds; }
    }
}
