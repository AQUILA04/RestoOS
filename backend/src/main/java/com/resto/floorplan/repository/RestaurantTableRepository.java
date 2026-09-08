package com.resto.floorplan.repository;

import com.resto.floorplan.domain.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, UUID> {
    List<RestaurantTable> findByStoreId(UUID storeId);
    List<RestaurantTable> findByZoneId(UUID zoneId);
    Optional<RestaurantTable> findByStoreIdAndTableNumber(UUID storeId, String tableNumber);
}
