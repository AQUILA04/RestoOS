package com.resto.kitchen.service;

import com.resto.order.domain.Order;
import com.resto.order.repository.OrderRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class KitchenService {

    private final OrderRepository orderRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public KitchenService(OrderRepository orderRepository, SimpMessagingTemplate messagingTemplate) {
        this.orderRepository = orderRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public Order updateKitchenOrderStatus(UUID orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        // Broadcast to KDS topic: /topic/store/{storeId}/kitchen
        String destination = "/topic/store/" + order.getStoreId() + "/kitchen";
        messagingTemplate.convertAndSend(destination, Map.of(
                "type", "KITCHEN_ORDER_UPDATE",
                "orderId", order.getId(),
                "orderNumber", order.getOrderNumber(),
                "status", newStatus
        ));

        return updatedOrder;
    }

    @Transactional(readOnly = true)
    public List<Order> getKitchenQueueForStore(UUID storeId) {
        return orderRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
    }
}
