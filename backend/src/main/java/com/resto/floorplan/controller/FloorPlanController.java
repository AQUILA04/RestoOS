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

    public static class CreateZoneRequest {
        private UUID organizationId;
        private UUID storeId;
        private String name;
        private Integer displayOrder;

        public UUID getOrganizationId() { return organizationId; }
        public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
        public UUID getStoreId() { return storeId; }
        public void setStoreId(UUID storeId) { this.storeId = storeId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    }

    public static class CreateTableRequest {
        private UUID organizationId;
        private UUID storeId;
        private UUID zoneId;
        private String tableNumber;
        private Integer capacity;
        private Integer posX;
        private Integer posY;
        private String shape;

        public UUID getOrganizationId() { return organizationId; }
        public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
        public UUID getStoreId() { return storeId; }
        public void setStoreId(UUID storeId) { this.storeId = storeId; }
        public UUID getZoneId() { return zoneId; }
        public void setZoneId(UUID zoneId) { this.zoneId = zoneId; }
        public String getTableNumber() { return tableNumber; }
        public void setTableNumber(String tableNumber) { this.tableNumber = tableNumber; }
        public Integer getCapacity() { return capacity; }
        public void setCapacity(Integer capacity) { this.capacity = capacity; }
        public Integer getPosX() { return posX; }
        public void setPosX(Integer posX) { this.posX = posX; }
        public Integer getPosY() { return posY; }
        public void setPosY(Integer posY) { this.posY = posY; }
        public String getShape() { return shape; }
        public void setShape(String shape) { this.shape = shape; }
    }

    public static class UpdateStatusRequest {
        private String status;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
