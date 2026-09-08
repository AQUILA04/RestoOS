package com.resto.kitchen.controller;

import com.resto.core.response.Response;
import com.resto.core.security.JwtAuth;
import com.resto.core.security.TenantContext;
import com.resto.kitchen.service.KitchenService;
import com.resto.order.domain.Order;
import com.resto.order.domain.OrderItem;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kitchen")
public class KitchenController {

    private final KitchenService kitchenService;

    public KitchenController(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @PatchMapping("/orders/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'KITCHEN')")
    public Response<Order> updateOrderStatus(@PathVariable("id") UUID orderId,
                                              @RequestBody UpdateKitchenStatusRequest request) {
        UUID actor = TenantContext.getUserId() != null ? TenantContext.getUserId() : JwtAuth.userId();
        Order order = kitchenService.updateKitchenOrderStatus(orderId, request.getStatus(), actor);
        return ok(order);
    }

    @PatchMapping("/orders/{id}/items/{itemId}/toggle")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'KITCHEN')")
    public Response<OrderItem> toggleItem(@PathVariable("id") UUID orderId,
                                          @PathVariable("itemId") UUID itemId) {
        UUID actor = TenantContext.getUserId() != null ? TenantContext.getUserId() : JwtAuth.userId();
        return ok(kitchenService.toggleItemPrepared(orderId, itemId, actor));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'KITCHEN')")
    public Response<List<Order>> getKitchenQueue(@RequestParam("storeId") UUID storeId) {
        return ok(kitchenService.getKitchenQueueForStore(storeId));
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

    public static class UpdateKitchenStatusRequest {
        private String status;
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
