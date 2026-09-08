package com.resto.order.controller;

import com.resto.core.response.Response;
import com.resto.order.domain.Order;
import com.resto.order.service.OrderService;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER')")
    public Response<Order> createOrder(@RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(
                request.getOrganizationId(),
                request.getStoreId(),
                request.getTableId(),
                request.getOrderType(),
                request.getNotes(),
                request.getItems()
        );
        return Response.<Order>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(order)
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER', 'KITCHEN')")
    public Response<List<Order>> getOrdersByStore(@RequestParam("storeId") UUID storeId) {
        List<Order> orders = orderService.getOrdersByStore(storeId);
        return Response.<List<Order>>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(orders)
                .build();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER')")
    public Response<Order> cancelOrder(@PathVariable("id") UUID orderId,
                                       @RequestBody CancelOrderRequest request) {
        Order order = orderService.cancelOrder(orderId, request.getUserId(), request.getReason());
        return Response.<Order>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(order)
                .build();
    }

    @Data
    public static class CancelOrderRequest {
        private UUID userId;
        private String reason;
    }

    @Data
    public static class CreateOrderRequest {
        private UUID organizationId;
        private UUID storeId;
        private UUID tableId;
        private String orderType;
        private String notes;
        private List<OrderService.CreateOrderItemParam> items;
    }
}
