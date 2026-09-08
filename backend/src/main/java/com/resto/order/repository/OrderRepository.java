package com.resto.order.repository;

import com.resto.order.domain.Order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByStoreIdOrderByCreatedAtDesc(UUID storeId);
    List<Order> findByStoreIdAndStatus(UUID storeId, String status);

    @Query("SELECT COALESCE(MAX(o.orderNumber), 100) + 1 FROM Order o WHERE o.storeId = :storeId")
    Integer getNextOrderNumber(@Param("storeId") UUID storeId);
}
