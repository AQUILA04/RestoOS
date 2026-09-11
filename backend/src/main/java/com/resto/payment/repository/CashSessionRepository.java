package com.resto.payment.repository;

import com.resto.payment.domain.CashSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashSessionRepository extends JpaRepository<CashSession, UUID> {

    Optional<CashSession> findByStoreIdAndOpenedByUserIdAndStatus(UUID storeId, UUID openedByUserId, String status);

    List<CashSession> findByStoreIdOrderByOpenedAtDesc(UUID storeId);

    List<CashSession> findByStoreIdAndOpenedByUserIdOrderByOpenedAtDesc(UUID storeId, UUID openedByUserId);
}
