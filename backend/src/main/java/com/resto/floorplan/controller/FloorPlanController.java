package com.resto.floorplan.controller;

import com.resto.core.response.Response;
import com.resto.floorplan.domain.RestaurantTable;
import com.resto.floorplan.domain.Zone;
import com.resto.floorplan.service.FloorPlanService;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/floor-plan")
public class FloorPlanController {

    private final FloorPlanService floorPlanService;

    public FloorPlanController(FloorPlanService floorPlanService) {
        this.floorPlanService = floorPlanService;
    }

    @PostMapping("/zones")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER')")
    public Response<Zone> createZone(@RequestBody CreateZoneRequest request) {
        Zone zone = floorPlanService.createZone(
                request.getOrganizationId(),
                request.getStoreId(),
                request.getName(),
                request.getDisplayOrder()
        );
        return Response.<Zone>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(zone)
                .build();
    }

    @GetMapping("/zones")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'WAITER', 'CASHIER')")
    public Response<List<Zone>> getZonesByStore(@RequestParam("storeId") UUID storeId) {
        List<Zone> zones = floorPlanService.getZonesByStore(storeId);
        return Response.<List<Zone>>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(zones)
                .build();
    }

    @PostMapping("/tables")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER')")
    public Response<RestaurantTable> createTable(@RequestBody CreateTableRequest request) {
        RestaurantTable table = floorPlanService.createTable(
                request.getOrganizationId(),
                request.getStoreId(),
                request.getZoneId(),
                request.getTableNumber(),
                request.getCapacity(),
                request.getPosX(),
                request.getPosY(),
                request.getShape()
        );
        return Response.<RestaurantTable>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(table)
                .build();
    }

    @GetMapping("/tables")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'WAITER', 'CASHIER')")
    public Response<List<RestaurantTable>> getTablesByStore(@RequestParam("storeId") UUID storeId) {
        List<RestaurantTable> tables = floorPlanService.getTablesByStore(storeId);
        return Response.<List<RestaurantTable>>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(tables)
                .build();
    }

    @PatchMapping("/tables/{tableId}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'WAITER', 'CASHIER')")
    public Response<RestaurantTable> updateTableStatus(@PathVariable("tableId") UUID tableId,
                                                        @RequestBody UpdateStatusRequest request) {
        RestaurantTable table = floorPlanService.updateTableStatus(tableId, request.getStatus());
        return Response.<RestaurantTable>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(table)
                .build();
    }

    @Data
    public static class CreateZoneRequest {
        private UUID organizationId;
        private UUID storeId;
        private String name;
        private Integer displayOrder;
    }

    @Data
    public static class CreateTableRequest {
        private UUID organizationId;
        private UUID storeId;
        private UUID zoneId;
        private String tableNumber;
        private Integer capacity;
        private Integer posX;
        private Integer posY;
        private String shape;
    }

    @Data
    public static class UpdateStatusRequest {
        private String status;
    }
}
