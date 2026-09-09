package com.resto.order.service;

import com.resto.audit.service.AuditService;
import com.resto.catalog.domain.ModifierOption;
import com.resto.catalog.domain.Product;
import com.resto.catalog.domain.StoreProduct;
import com.resto.catalog.repository.ModifierOptionRepository;
import com.resto.catalog.repository.ProductRepository;
import com.resto.catalog.repository.StoreProductRepository;
import com.resto.core.outbox.OutboxService;
import com.resto.core.security.TenantContext;
import com.resto.floorplan.domain.RestaurantTable;
import com.resto.floorplan.repository.RestaurantTableRepository;
import com.resto.order.domain.Order;
import com.resto.order.domain.OrderItem;
import com.resto.order.domain.OrderItemModifier;
import com.resto.order.domain.OrderStateMachine;
import com.resto.order.repository.OrderCounterRepository;
import com.resto.order.repository.OrderRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderCounterRepository orderCounterRepository;
    private final ProductRepository productRepository;
    private final StoreProductRepository storeProductRepository;
    private final ModifierOptionRepository modifierOptionRepository;
    private final RestaurantTableRepository tableRepository;
    private final AuditService auditService;
    private final OutboxService outboxService;
    private final SimpMessagingTemplate messagingTemplate;

    public OrderService(OrderRepository orderRepository,
                        OrderCounterRepository orderCounterRepository,
                        ProductRepository productRepository,
                        StoreProductRepository storeProductRepository,
                        ModifierOptionRepository modifierOptionRepository,
                        RestaurantTableRepository tableRepository,
                        AuditService auditService,
                        OutboxService outboxService,
                        SimpMessagingTemplate messagingTemplate) {
        this.orderRepository = orderRepository;
        this.orderCounterRepository = orderCounterRepository;
        this.productRepository = productRepository;
        this.storeProductRepository = storeProductRepository;
        this.modifierOptionRepository = modifierOptionRepository;
        this.tableRepository = tableRepository;
        this.auditService = auditService;
        this.outboxService = outboxService;
        this.messagingTemplate = messagingTemplate;
    }

    public Order createOrder(UUID organizationId, UUID storeId, UUID tableId, String orderType,
                             String notes, List<CreateOrderItemParam> itemParams, boolean sendToKitchen,
                             UUID actorUserId) {
        if (itemParams == null || itemParams.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        String resolvedType = orderType != null ? orderType : "DINE_IN";
        if ("DINE_IN".equals(resolvedType) && tableId != null) {
            RestaurantTable table = tableRepository.findById(tableId)
                    .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId));
            if (!storeId.equals(table.getStoreId())) {
                throw new IllegalArgumentException("Table does not belong to store");
            }
            if ("OUT_OF_SERVICE".equals(table.getStatus())) {
                throw new IllegalArgumentException("Table is out of service");
            }
        }

        int nextOrderNumber = nextOrderNumber(organizationId, storeId);
        String initialStatus = sendToKitchen ? "SENT_TO_KITCHEN" : "CREATED";

        Order order = Order.builder()
                .organizationId(organizationId)
                .storeId(storeId)
                .tableId(tableId)
                .createdBy(actorUserId)
                .orderNumber(nextOrderNumber)
                .orderType(resolvedType)
                .status(initialStatus)
                .paymentStatus("UNPAID")
                .notes(notes)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;

        for (CreateOrderItemParam itemParam : itemParams) {
            Product product = productRepository.findById(itemParam.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemParam.getProductId()));

            if (!organizationId.equals(product.getOrganizationId())) {
                throw new IllegalArgumentException("Product does not belong to organization");
            }
            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new IllegalArgumentException("Product is inactive: " + product.getName());
            }

            StoreProduct storeProduct = storeProductRepository.findByStoreIdAndProductId(storeId, product.getId()).orElse(null);
            if (storeProduct != null && Boolean.FALSE.equals(storeProduct.getAvailable())) {
                throw new IllegalArgumentException("Product unavailable at store (86): " + product.getName());
            }
            if (Boolean.TRUE.equals(product.getIs86()) && (storeProduct == null || Boolean.FALSE.equals(storeProduct.getAvailable()))) {
                throw new IllegalArgumentException("Product is out of stock (86): " + product.getName());
            }

            BigDecimal unitPrice = (storeProduct != null && storeProduct.getOverridePrice() != null)
                    ? storeProduct.getOverridePrice()
                    : product.getBasePrice();

            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(itemParam.getQuantity()));

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
                    .prepared(false)
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
                    itemSubtotal = itemSubtotal.add(priceDelta.multiply(BigDecimal.valueOf(itemParam.getQuantity())));
                }
            }

            orderItem.setSubtotal(itemSubtotal);
            order.getItems().add(orderItem);

            BigDecimal itemTax = itemSubtotal.multiply(product.getTaxRate())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(itemSubtotal);
            taxTotal = taxTotal.add(itemTax);
        }

        order.setSubtotal(subtotal);
        order.setTaxTotal(taxTotal);
        order.setTotalAmount(subtotal.add(taxTotal));

        Order saved = orderRepository.save(order);

        if ("DINE_IN".equals(resolvedType) && tableId != null) {
            occupyTable(tableId);
        }

        auditService.record(organizationId, storeId, actorUserId, "ORDER_CREATED", "ORDER", saved.getId(),
                "Order #" + saved.getOrderNumber() + " created status=" + saved.getStatus());

        if (sendToKitchen) {
            publishOrderCreated(saved);
        }

        return saved;
    }

    public Order sendToKitchen(UUID orderId, UUID actorUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        OrderStateMachine.assertTransition(order.getStatus(), "SENT_TO_KITCHEN");
        order.setStatus("SENT_TO_KITCHEN");
        Order saved = orderRepository.save(order);
        publishOrderCreated(saved);
        auditService.record(order.getOrganizationId(), order.getStoreId(), actorUserId,
                "ORDER_SENT_TO_KITCHEN", "ORDER", orderId, "Sent to kitchen");
        return saved;
    }

    public Order deliver(UUID orderId, UUID actorUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        OrderStateMachine.assertTransition(order.getStatus(), "DELIVERED");
        order.setStatus("DELIVERED");
        Order saved = orderRepository.save(order);
        auditService.record(order.getOrganizationId(), order.getStoreId(), actorUserId,
                "ORDER_DELIVERED", "ORDER", orderId, "Marked delivered");
        notifyPos(saved, "ORDER_DELIVERED");
        return saved;
    }

    /**
     * Assign or change the occupied table after order creation (DINE_IN).
     * Table is optional at creation and can be set later.
     */
    public Order updateOrderTable(UUID orderId, UUID tableId, UUID actorUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!"DINE_IN".equals(order.getOrderType())) {
            throw new IllegalArgumentException("Table can only be set on DINE_IN orders");
        }
        if ("CLOSED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new IllegalStateException("Cannot update table for order in status: " + order.getStatus());
        }

        UUID previousTableId = order.getTableId();

        if (tableId == null) {
            order.setTableId(null);
            Order saved = orderRepository.save(order);
            if (previousTableId != null) {
                releaseTableIfIdle(order.getStoreId(), previousTableId);
            }
            auditService.record(order.getOrganizationId(), order.getStoreId(), actorUserId,
                    "ORDER_TABLE_CLEARED", "ORDER", orderId, "Table cleared");
            return saved;
        }

        RestaurantTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId));
        if (!order.getStoreId().equals(table.getStoreId())) {
            throw new IllegalArgumentException("Table does not belong to store");
        }
        if ("OUT_OF_SERVICE".equals(table.getStatus())) {
            throw new IllegalArgumentException("Table is out of service");
        }

        order.setTableId(tableId);
        Order saved = orderRepository.save(order);
        occupyTable(tableId);
        if (previousTableId != null && !previousTableId.equals(tableId)) {
            releaseTableIfIdle(order.getStoreId(), previousTableId);
        }
        auditService.record(order.getOrganizationId(), order.getStoreId(), actorUserId,
                "ORDER_TABLE_ASSIGNED", "ORDER", orderId, "tableId=" + tableId);
        return saved;
    }

    public Order cancelOrder(UUID orderId, UUID userId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Cancellation reason is required");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!OrderStateMachine.canCancel(order.getStatus())) {
            throw new IllegalStateException("Cannot cancel order in status: " + order.getStatus());
        }

        order.setStatus("CANCELLED");
        order.setCancelledAt(OffsetDateTime.now());
        order.setCancelledBy(userId);
        order.setCancellationReason(reason);
        Order saved = orderRepository.save(order);

        if (order.getTableId() != null) {
            releaseTableIfIdle(order.getStoreId(), order.getTableId());
        }

        auditService.record(order.getOrganizationId(), order.getStoreId(), userId,
                "ORDER_CANCELLED", "ORDER", orderId, "Reason: " + reason);
        return saved;
    }

    public void releaseTableIfIdle(UUID storeId, UUID tableId) {
        long open = orderRepository.countOpenDineInForTable(storeId, tableId);
        if (open == 0) {
            tableRepository.findById(tableId).ifPresent(t -> {
                t.setStatus("AVAILABLE");
                tableRepository.save(t);
            });
        }
    }

    private void occupyTable(UUID tableId) {
        tableRepository.findById(tableId).ifPresent(t -> {
            t.setStatus("OCCUPIED");
            tableRepository.save(t);
        });
    }

    private int nextOrderNumber(UUID organizationId, UUID storeId) {
        Integer next = orderCounterRepository.allocateNextOrderNumber(storeId, organizationId);
        if (next == null) {
            throw new IllegalStateException("Failed to allocate order number for store " + storeId);
        }
        return next;
    }

    private void publishOrderCreated(Order order) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", order.getId().toString());
        payload.put("orderNumber", order.getOrderNumber());
        payload.put("storeId", order.getStoreId().toString());
        payload.put("status", order.getStatus());
        outboxService.enqueue(order.getOrganizationId(), order.getStoreId(),
                "ORDER", order.getId(), "ORDER_CREATED", payload);

        afterCommit(() -> {
            String kitchen = "/topic/store/" + order.getStoreId() + "/kitchen";
            String pos = "/topic/store/" + order.getStoreId() + "/pos";
            messagingTemplate.convertAndSend(kitchen, Map.of(
                    "type", "ORDER_CREATED",
                    "orderId", order.getId().toString(),
                    "orderNumber", order.getOrderNumber(),
                    "status", order.getStatus()
            ));
            messagingTemplate.convertAndSend(pos, Map.of(
                    "type", "ORDER_CREATED",
                    "orderId", order.getId().toString(),
                    "orderNumber", order.getOrderNumber(),
                    "status", order.getStatus()
            ));
        });
    }

    private void notifyPos(Order order, String type) {
        afterCommit(() -> messagingTemplate.convertAndSend(
                "/topic/store/" + order.getStoreId() + "/pos",
                Map.of("type", type, "orderId", order.getId().toString(), "status", order.getStatus())
        ));
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByStore(UUID storeId) {
        return orderRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        UUID orgId = TenantContext.getOrgId();
        if (orgId != null && !orgId.equals(order.getOrganizationId())) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        return order;
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
