package com.resto.order.controller;

import com.resto.core.idempotency.IdempotencyService;
import com.resto.core.response.Response;
import com.resto.core.security.JwtAuth;
import com.resto.core.security.TenantContext;
import com.resto.order.domain.Order;
import com.resto.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    public OrderController(OrderService orderService, IdempotencyService idempotencyService) {
        this.orderService = orderService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER')")
    public Response<Order> createOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Tenant-ID", required = false) UUID ignoredTenantHeader,
            @RequestBody CreateOrderRequest request) {

        UUID organizationId = TenantContext.getOrgId() != null ? TenantContext.getOrgId() : JwtAuth.organizationId();
        UUID storeId = request.getStoreId() != null ? request.getStoreId()
                : JwtAuth.storeId().orElseThrow(() -> new IllegalArgumentException("storeId required"));
        UUID actor = TenantContext.getUserId() != null ? TenantContext.getUserId() : JwtAuth.userId();
        boolean sendToKitchen = Boolean.TRUE.equals(request.getSendToKitchen());

        Order order = idempotencyService.execute(
                organizationId,
                storeId,
                "POST /api/v1/orders",
                idempotencyKey,
                request,
                () -> orderService.createOrder(
                        organizationId,
                        storeId,
                        request.getTableId(),
                        request.getOrderType(),
                        request.getNotes(),
                        request.getItems(),
                        sendToKitchen,
                        actor
                ),
                Order.class
        );

        return ok(order);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER', 'KITCHEN')")
    public Response<List<Order>> getOrdersByStore(@RequestParam("storeId") UUID storeId) {
        return ok(orderService.getOrdersByStore(storeId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER', 'KITCHEN')")
    public Response<Order> getOrder(@PathVariable("id") UUID id) {
        return ok(orderService.getOrder(id));
    }

    @PostMapping("/{id}/send-to-kitchen")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER')")
    public Response<Order> sendToKitchen(@PathVariable("id") UUID orderId) {
        UUID actor = TenantContext.getUserId() != null ? TenantContext.getUserId() : JwtAuth.userId();
        return ok(orderService.sendToKitchen(orderId, actor));
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER')")
    public Response<Order> deliver(@PathVariable("id") UUID orderId) {
        UUID actor = TenantContext.getUserId() != null ? TenantContext.getUserId() : JwtAuth.userId();
        return ok(orderService.deliver(orderId, actor));
    }

    @PatchMapping("/{id}/table")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER')")
    public Response<Order> updateOrderTable(@PathVariable("id") UUID orderId,
                                            @RequestBody UpdateTableRequest request) {
        UUID actor = TenantContext.getUserId() != null ? TenantContext.getUserId() : JwtAuth.userId();
        return ok(orderService.updateOrderTable(orderId, request.getTableId(), actor));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER')")
    public Response<Order> cancelOrder(@PathVariable("id") UUID orderId,
                                       @RequestBody CancelOrderRequest request) {
        UUID actor = TenantContext.getUserId() != null ? TenantContext.getUserId() : JwtAuth.userId();
        return ok(orderService.cancelOrder(orderId, actor, request.getReason()));
    }

    private <T> Response<T> ok(T data) {
        return Response.<T>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(data)
                .build();
    }

    public static class CancelOrderRequest {
        private String reason;
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class UpdateTableRequest {
        private UUID tableId;
        public UUID getTableId() { return tableId; }
        public void setTableId(UUID tableId) { this.tableId = tableId; }
    }

    public static class CreateOrderRequest {
        private UUID storeId;
        private UUID tableId;
        private String orderType;
        private String notes;
        private Boolean sendToKitchen;
        private List<OrderService.CreateOrderItemParam> items;

        public UUID getStoreId() { return storeId; }
        public void setStoreId(UUID storeId) { this.storeId = storeId; }
        public UUID getTableId() { return tableId; }
        public void setTableId(UUID tableId) { this.tableId = tableId; }
        public String getOrderType() { return orderType; }
        public void setOrderType(String orderType) { this.orderType = orderType; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public Boolean getSendToKitchen() { return sendToKitchen; }
        public void setSendToKitchen(Boolean sendToKitchen) { this.sendToKitchen = sendToKitchen; }
        public List<OrderService.CreateOrderItemParam> getItems() { return items; }
        public void setItems(List<OrderService.CreateOrderItemParam> items) { this.items = items; }
    }
}
