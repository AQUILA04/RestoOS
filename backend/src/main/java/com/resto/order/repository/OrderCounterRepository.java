package com.resto.order.repository;

import com.resto.order.domain.OrderCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderCounterRepository extends JpaRepository<OrderCounter, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM OrderCounter c WHERE c.storeId = :storeId")
    Optional<OrderCounter> findByStoreIdForUpdate(@Param("storeId") UUID storeId);
}
