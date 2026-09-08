package com.resto.floorplan.controller;

import com.resto.core.response.Response;
import com.resto.core.security.JwtAuth;
import com.resto.core.security.TenantContext;
import com.resto.floorplan.domain.RestaurantTable;
import com.resto.floorplan.domain.Zone;
import com.resto.floorplan.service.FloorPlanService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stores/{storeId}")
public class FloorPlanController {

    private static final Set<String> TABLE_STATUSES = Set.of("AVAILABLE", "OCCUPIED", "RESERVED", "OUT_OF_SERVICE");

    private final FloorPlanService floorPlanService;

    public FloorPlanController(FloorPlanService floorPlanService) {
        this.floorPlanService = floorPlanService;
    }

    @PostMapping("/zones")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER')")
    public Response<Zone> createZone(@PathVariable("storeId") UUID storeId,
                                     @RequestBody CreateZoneRequest request) {
        UUID organizationId = resolveOrg(request.getOrganizationId());
        Zone zone = floorPlanService.createZone(organizationId, storeId, request.getName(), request.getDisplayOrder());
        return ok(zone);
    }

    @GetMapping("/zones")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'WAITER', 'CASHIER')")
    public Response<List<Zone>> getZones(@PathVariable("storeId") UUID storeId) {
        return ok(floorPlanService.getZonesByStore(storeId));
    }

    @PostMapping("/tables")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER')")
    public Response<RestaurantTable> createTable(@PathVariable("storeId") UUID storeId,
                                                 @RequestBody CreateTableRequest request) {
        UUID organizationId = resolveOrg(request.getOrganizationId());
        UUID zoneId = request.getZoneId();
        // Accept zone name (e.g. golden path: { zone: "Salle", name: "Table 05", capacity: 4 })
        // and auto-create the zone when it does not exist yet.
        if (zoneId == null && request.getZone() != null && !request.getZone().isBlank()) {
            Zone zone = floorPlanService.findOrCreateZone(organizationId, storeId, request.getZone().trim());
            zoneId = zone.getId();
        }
        if (zoneId == null) {
            throw new IllegalArgumentException("zoneId or zone name is required");
        }
        String tableNumber = request.getTableNumber() != null ? request.getTableNumber() : request.getName();
        RestaurantTable table = floorPlanService.createTable(
                organizationId, storeId, zoneId, tableNumber,
                request.getCapacity(), request.getPosX(), request.getPosY(), request.getShape()
        );
        return ok(table);
    }

    @GetMapping("/tables")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'WAITER', 'CASHIER')")
    public Response<List<RestaurantTable>> getTables(@PathVariable("storeId") UUID storeId) {
        return ok(floorPlanService.getTablesByStore(storeId));
    }

    @PatchMapping("/tables/{tableId}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'WAITER', 'CASHIER')")
    public Response<RestaurantTable> updateTableStatus(@PathVariable("storeId") UUID storeId,
                                                       @PathVariable("tableId") UUID tableId,
                                                       @RequestBody UpdateStatusRequest request) {
        if (request.getStatus() == null || !TABLE_STATUSES.contains(request.getStatus())) {
            throw new IllegalArgumentException("Invalid table status: " + request.getStatus());
        }
        return ok(floorPlanService.updateTableStatus(tableId, request.getStatus()));
    }

    private UUID resolveOrg(UUID requestOrg) {
        if (TenantContext.getOrgId() != null) {
            return TenantContext.getOrgId();
        }
        try {
            return JwtAuth.organizationId();
        } catch (Exception e) {
            if (requestOrg != null) {
                return requestOrg;
            }
            throw new IllegalStateException("organization_id required");
        }
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

    public static class CreateZoneRequest {
        private UUID organizationId;
        private String name;
        private Integer displayOrder;
        public UUID getOrganizationId() { return organizationId; }
        public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    }

    public static class CreateTableRequest {
        private UUID organizationId;
        private UUID zoneId;
        private String tableNumber;
        private String name;
        private String zone;
        private Integer capacity;
        private Integer posX;
        private Integer posY;
        private String shape;
        public UUID getOrganizationId() { return organizationId; }
        public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
        public UUID getZoneId() { return zoneId; }
        public void setZoneId(UUID zoneId) { this.zoneId = zoneId; }
        public String getTableNumber() { return tableNumber; }
        public void setTableNumber(String tableNumber) { this.tableNumber = tableNumber; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getZone() { return zone; }
        public void setZone(String zone) { this.zone = zone; }
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
