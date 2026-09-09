package com.resto.order.repository;

import com.resto.order.domain.OrderCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderCounterRepository extends JpaRepository<OrderCounter, UUID> {

    /**
     * Atomically allocate the next store-local order number.
     * Avoids duplicate-key races when the first order for a store is created concurrently.
     */
    @Query(value = """
            INSERT INTO order_counters (store_id, organization_id, last_order_number, updated_at)
            VALUES (:storeId, :orgId, 101, NOW())
            ON CONFLICT (store_id) DO UPDATE
              SET last_order_number = order_counters.last_order_number + 1,
                  updated_at = NOW()
            RETURNING last_order_number
            """, nativeQuery = true)
    Integer allocateNextOrderNumber(@Param("storeId") UUID storeId, @Param("orgId") UUID orgId);
}
