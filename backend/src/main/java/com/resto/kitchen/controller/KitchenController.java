package com.resto.kitchen.controller;

import com.resto.core.response.Response;
import com.resto.kitchen.service.KitchenService;
import com.resto.order.domain.Order;
import lombok.Data;
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
        Order order = kitchenService.updateKitchenOrderStatus(orderId, request.getStatus());
        return Response.<Order>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(order)
                .build();
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'KITCHEN')")
    public Response<List<Order>> getKitchenQueue(@RequestParam("storeId") UUID storeId) {
        List<Order> orders = kitchenService.getKitchenQueueForStore(storeId);
        return Response.<List<Order>>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(orders)
                .build();
    }

    @Data
    public static class UpdateKitchenStatusRequest {
        private String status;
    }
}
