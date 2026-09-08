package com.resto.order.repository;

import com.resto.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByStoreIdOrderByCreatedAtDesc(UUID storeId);
    List<Order> findByStoreIdAndStatus(UUID storeId, String status);

    @Query("SELECT o FROM Order o WHERE o.storeId = :storeId AND o.status IN :statuses ORDER BY o.createdAt ASC")
    List<Order> findKitchenQueue(@Param("storeId") UUID storeId, @Param("statuses") List<String> statuses);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.storeId = :storeId AND o.tableId = :tableId AND o.status NOT IN ('CLOSED', 'CANCELLED')")
    long countOpenDineInForTable(@Param("storeId") UUID storeId, @Param("tableId") UUID tableId);

    List<Order> findByStoreIdAndCreatedAtGreaterThanEqual(UUID storeId, OffsetDateTime start);

    @Query("SELECT COALESCE(MAX(o.orderNumber), 100) + 1 FROM Order o WHERE o.storeId = :storeId")
    Integer getNextOrderNumber(@Param("storeId") UUID storeId);
}
