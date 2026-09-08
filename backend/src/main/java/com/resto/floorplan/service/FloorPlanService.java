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
}
