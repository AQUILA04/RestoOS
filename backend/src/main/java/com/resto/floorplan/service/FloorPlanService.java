package com.resto.floorplan.service;

import com.resto.floorplan.domain.RestaurantTable;
import com.resto.floorplan.domain.Zone;
import com.resto.floorplan.repository.RestaurantTableRepository;
import com.resto.floorplan.repository.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FloorPlanService {

    private final ZoneRepository zoneRepository;
    private final RestaurantTableRepository tableRepository;

    public FloorPlanService(ZoneRepository zoneRepository, RestaurantTableRepository tableRepository) {
        this.zoneRepository = zoneRepository;
        this.tableRepository = tableRepository;
    }

    public Zone createZone(UUID organizationId, UUID storeId, String name, Integer displayOrder) {
        Zone zone = Zone.builder()
                .organizationId(organizationId)
                .storeId(storeId)
                .name(name)
                .displayOrder(displayOrder != null ? displayOrder : 0)
                .build();
        return zoneRepository.save(zone);
    }

    @Transactional(readOnly = true)
    public List<Zone> getZonesByStore(UUID storeId) {
        return zoneRepository.findByStoreIdOrderByDisplayOrderAsc(storeId);
    }

    /**
     * Finds an existing zone by name within a store, or creates one if it does not exist.
     * Used by the E2E-friendly table endpoint to avoid requiring a separate zone creation step.
     */
    public Zone findOrCreateZone(UUID organizationId, UUID storeId, String zoneName) {
        return zoneRepository.findByStoreIdAndName(storeId, zoneName)
                .orElseGet(() -> createZone(organizationId, storeId, zoneName, 0));
    }

    public RestaurantTable createTable(UUID organizationId, UUID storeId, UUID zoneId, String tableNumber, Integer capacity, Integer posX, Integer posY, String shape) {
        if (!zoneRepository.existsById(zoneId)) {
            throw new IllegalArgumentException("Zone not found: " + zoneId);
        }
        RestaurantTable table = RestaurantTable.builder()
                .organizationId(organizationId)
                .storeId(storeId)
                .zoneId(zoneId)
                .tableNumber(tableNumber)
                .capacity(capacity != null ? capacity : 4)
                .posX(posX != null ? posX : 0)
                .posY(posY != null ? posY : 0)
                .shape(shape != null ? shape : "SQUARE")
                .status("AVAILABLE")
                .build();
        return tableRepository.save(table);
    }

    @Transactional(readOnly = true)
    public List<RestaurantTable> getTablesByStore(UUID storeId) {
        return tableRepository.findByStoreId(storeId);
    }

    public RestaurantTable updateTableStatus(UUID tableId, String status) {
        RestaurantTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId));
        table.setStatus(status);
        return tableRepository.save(table);
    }

    public RestaurantTable updateTable(UUID tableId, UUID zoneId, String tableNumber, Integer capacity,
                                       Integer posX, Integer posY, String shape, String status) {
        RestaurantTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId));
        if (zoneId != null) {
            if (!zoneRepository.existsById(zoneId)) {
                throw new IllegalArgumentException("Zone not found: " + zoneId);
            }
            table.setZoneId(zoneId);
        }
        if (tableNumber != null && !tableNumber.isBlank()) {
            table.setTableNumber(tableNumber.trim());
        }
        if (capacity != null) {
            if (capacity < 1) {
                throw new IllegalArgumentException("capacity must be >= 1");
            }
            table.setCapacity(capacity);
        }
        if (posX != null) table.setPosX(posX);
        if (posY != null) table.setPosY(posY);
        if (shape != null && !shape.isBlank()) table.setShape(shape.trim());
        if (status != null && !status.isBlank()) table.setStatus(status.trim());
        return tableRepository.save(table);
    }

    public void deleteTable(UUID tableId) {
        if (!tableRepository.existsById(tableId)) {
            throw new IllegalArgumentException("Table not found: " + tableId);
        }
        tableRepository.deleteById(tableId);
    }

    public Zone updateZone(UUID zoneId, String name, Integer displayOrder) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new IllegalArgumentException("Zone not found: " + zoneId));
        if (name != null && !name.isBlank()) zone.setName(name.trim());
        if (displayOrder != null) zone.setDisplayOrder(displayOrder);
        return zoneRepository.save(zone);
    }

    public void deleteZone(UUID zoneId) {
        if (!zoneRepository.existsById(zoneId)) {
            throw new IllegalArgumentException("Zone not found: " + zoneId);
        }
        List<RestaurantTable> tables = tableRepository.findByZoneId(zoneId);
        if (!tables.isEmpty()) {
            throw new IllegalArgumentException("Cannot delete zone with tables; remove tables first");
        }
        zoneRepository.deleteById(zoneId);
    }
}
