package com.resto.unit;

import com.resto.audit.service.AuditService;
import com.resto.catalog.domain.Product;
import com.resto.catalog.repository.ModifierOptionRepository;
import com.resto.catalog.repository.ProductRepository;
import com.resto.catalog.repository.StoreProductRepository;
import com.resto.core.outbox.OutboxService;
import com.resto.floorplan.domain.RestaurantTable;
import com.resto.floorplan.repository.RestaurantTableRepository;
import com.resto.order.domain.Order;
import com.resto.order.repository.OrderCounterRepository;
import com.resto.order.repository.OrderRepository;
import com.resto.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderUnitTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderCounterRepository orderCounterRepository;
    @Mock ProductRepository productRepository;
    @Mock StoreProductRepository storeProductRepository;
    @Mock ModifierOptionRepository modifierOptionRepository;
    @Mock RestaurantTableRepository tableRepository;
    @Mock AuditService auditService;
    @Mock OutboxService outboxService;
    @Mock SimpMessagingTemplate messagingTemplate;

    OrderService orderService;

    UUID orgId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();
    UUID tableId = UUID.randomUUID();
    UUID productId = UUID.randomUUID();
    UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository, orderCounterRepository, productRepository,
                storeProductRepository, modifierOptionRepository, tableRepository,
                auditService, outboxService, messagingTemplate
        );
    }

    @Test
    @DisplayName("createOrder snapshots prices, occupies table, optional sendToKitchen")
    void createOrderSendToKitchen() {
        RestaurantTable table = new RestaurantTable();
        table.setId(tableId);
        table.setStoreId(storeId);
        table.setStatus("AVAILABLE");
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(table));

        when(orderCounterRepository.allocateNextOrderNumber(storeId, orgId)).thenReturn(101);

        Product product = Product.builder()
                .id(productId)
                .organizationId(orgId)
                .categoryId(UUID.randomUUID())
                .name("Burger")
                .basePrice(new BigDecimal("12.00"))
                .taxRate(new BigDecimal("10.00"))
                .active(true)
                .is86(false)
                .build();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(storeProductRepository.findByStoreIdAndProductId(storeId, productId)).thenReturn(Optional.empty());

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getId() == null) o.setId(UUID.randomUUID());
            return o;
        });

        OrderService.CreateOrderItemParam item = new OrderService.CreateOrderItemParam();
        item.setProductId(productId);
        item.setQuantity(2);

        Order order = orderService.createOrder(orgId, storeId, tableId, "DINE_IN", null,
                List.of(item), true, actorId);

        assertEquals("SENT_TO_KITCHEN", order.getStatus());
        assertEquals("UNPAID", order.getPaymentStatus());
        assertEquals(101, order.getOrderNumber());
        assertEquals(1, order.getItems().size());
        assertEquals("Burger", order.getItems().get(0).getProductName());
        assertEquals(new BigDecimal("12.00"), order.getItems().get(0).getUnitPrice());
        assertEquals(new BigDecimal("26.40"), order.getTotalAmount()); // 24 + 2.40 tax

        ArgumentCaptor<RestaurantTable> tableCap = ArgumentCaptor.forClass(RestaurantTable.class);
        verify(tableRepository, atLeastOnce()).save(tableCap.capture());
        assertEquals("OCCUPIED", tableCap.getValue().getStatus());
        verify(outboxService).enqueue(eq(orgId), eq(storeId), eq("ORDER"), any(), eq("ORDER_CREATED"), anyMap());
        verify(auditService).record(eq(orgId), eq(storeId), eq(actorId), eq("ORDER_CREATED"), any(), any(), any());
    }

    @Test
    @DisplayName("createOrder rejects 86 / unavailable product")
    void rejectsUnavailable() {
        Product product = Product.builder()
                .id(productId)
                .organizationId(orgId)
                .categoryId(UUID.randomUUID())
                .name("Soup")
                .basePrice(new BigDecimal("5.00"))
                .taxRate(new BigDecimal("10.00"))
                .active(true)
                .is86(false)
                .build();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        com.resto.catalog.domain.StoreProduct sp = com.resto.catalog.domain.StoreProduct.builder()
                .storeId(storeId).productId(productId).organizationId(orgId).available(false).build();
        when(storeProductRepository.findByStoreIdAndProductId(storeId, productId)).thenReturn(Optional.of(sp));

        when(orderCounterRepository.allocateNextOrderNumber(storeId, orgId)).thenReturn(101);

        OrderService.CreateOrderItemParam item = new OrderService.CreateOrderItemParam();
        item.setProductId(productId);
        item.setQuantity(1);

        assertThrows(IllegalArgumentException.class, () ->
                orderService.createOrder(orgId, storeId, null, "TAKEAWAY", null, List.of(item), false, actorId));
    }

    @Test
    @DisplayName("DINE_IN may be created without table; table can be assigned later")
    void dineInWithoutTableThenAssign() {
        when(orderCounterRepository.allocateNextOrderNumber(storeId, orgId)).thenReturn(102);

        Product product = Product.builder()
                .id(productId)
                .organizationId(orgId)
                .categoryId(UUID.randomUUID())
                .name("Burger")
                .basePrice(new BigDecimal("12.00"))
                .taxRate(new BigDecimal("10.00"))
                .active(true)
                .is86(false)
                .build();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(storeProductRepository.findByStoreIdAndProductId(storeId, productId)).thenReturn(Optional.empty());

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getId() == null) o.setId(UUID.randomUUID());
            return o;
        });

        OrderService.CreateOrderItemParam item = new OrderService.CreateOrderItemParam();
        item.setProductId(productId);
        item.setQuantity(1);

        Order order = orderService.createOrder(orgId, storeId, null, "DINE_IN", null,
                List.of(item), true, actorId);

        assertNull(order.getTableId());
        assertEquals(actorId, order.getCreatedBy());
        assertEquals("SENT_TO_KITCHEN", order.getStatus());
        verify(tableRepository, never()).save(any());

        RestaurantTable table = new RestaurantTable();
        table.setId(tableId);
        table.setStoreId(storeId);
        table.setStatus("AVAILABLE");
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(table));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        Order updated = orderService.updateOrderTable(order.getId(), tableId, actorId);
        assertEquals(tableId, updated.getTableId());
        ArgumentCaptor<RestaurantTable> tableCap = ArgumentCaptor.forClass(RestaurantTable.class);
        verify(tableRepository, atLeastOnce()).save(tableCap.capture());
        assertEquals("OCCUPIED", tableCap.getValue().getStatus());
    }

    @Test
    @DisplayName("cancel requires reason and allowed state")
    void cancelRules() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .storeId(storeId)
                .orderNumber(1)
                .status("READY")
                .build();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThrows(IllegalArgumentException.class,
                () -> orderService.cancelOrder(order.getId(), actorId, " "));
        assertThrows(IllegalStateException.class,
                () -> orderService.cancelOrder(order.getId(), actorId, "too late"));
    }
}
