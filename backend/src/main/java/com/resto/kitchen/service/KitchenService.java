package com.resto.kitchen.service;

import com.resto.audit.service.AuditService;
import com.resto.order.domain.Order;
import com.resto.order.domain.OrderItem;
import com.resto.order.domain.OrderStateMachine;
import com.resto.order.repository.OrderRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class KitchenService {

    private static final List<String> ACTIVE = List.of("SENT_TO_KITCHEN", "PREPARING", "READY");

    private final OrderRepository orderRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AuditService auditService;

    public KitchenService(OrderRepository orderRepository,
                          SimpMessagingTemplate messagingTemplate,
                          AuditService auditService) {
        this.orderRepository = orderRepository;
        this.messagingTemplate = messagingTemplate;
        this.auditService = auditService;
    }

    public Order updateKitchenOrderStatus(UUID orderId, String newStatus, UUID actorUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!ACTIVE.contains(order.getStatus()) && !"SENT_TO_KITCHEN".equals(newStatus)) {
            throw new IllegalStateException("Order not in kitchen queue: " + order.getStatus());
        }

        // Strict kitchen transitions: SENT_TO_KITCHEN → PREPARING → READY
        if (!List.of("PREPARING", "READY").contains(newStatus)) {
            throw new IllegalArgumentException("Kitchen may only set PREPARING or READY");
        }
        OrderStateMachine.assertTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);

        auditService.record(order.getOrganizationId(), order.getStoreId(), actorUserId,
                "KITCHEN_STATUS", "ORDER", orderId, "status=" + newStatus);

        afterCommit(() -> broadcast(updated, "KITCHEN_ORDER_UPDATE"));
        return updated;
    }

    public OrderItem toggleItemPrepared(UUID orderId, UUID itemId, UUID actorUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        OrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Order item not found: " + itemId));

        item.setPrepared(!Boolean.TRUE.equals(item.getPrepared()));
        orderRepository.save(order);

        auditService.record(order.getOrganizationId(), order.getStoreId(), actorUserId,
                "KITCHEN_ITEM_TOGGLE", "ORDER_ITEM", itemId, "prepared=" + item.getPrepared());

        afterCommit(() -> broadcast(order, "KITCHEN_ITEM_TOGGLE"));
        return item;
    }

    @Transactional(readOnly = true)
    public List<Order> getKitchenQueueForStore(UUID storeId) {
        List<Order> queue = orderRepository.findKitchenQueue(storeId, ACTIVE);
        // Initialize collections inside the transaction (avoid empty/lazy after commit under RLS).
        queue.forEach(order -> {
            if (order.getItems() != null) {
                order.getItems().forEach(item -> {
                    if (item.getModifiers() != null) {
                        item.getModifiers().size();
                    }
                });
            }
        });
        queue.sort((a, b) -> {
            if (a.getCreatedAt() == null || b.getCreatedAt() == null) {
                return 0;
            }
            return a.getCreatedAt().compareTo(b.getCreatedAt());
        });
        return queue;
    }

    private void broadcast(Order order, String type) {
        Map<String, Object> payload = Map.of(
                "type", type,
                "orderId", order.getId().toString(),
                "orderNumber", order.getOrderNumber(),
                "status", order.getStatus()
        );
        messagingTemplate.convertAndSend("/topic/store/" + order.getStoreId() + "/kitchen", payload);
        messagingTemplate.convertAndSend("/topic/store/" + order.getStoreId() + "/pos", payload);
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
}
