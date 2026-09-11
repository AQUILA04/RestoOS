package com.resto.payment.repository;

import com.resto.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByOrderId(UUID orderId);
    List<Payment> findByStoreId(UUID storeId);
    List<Payment> findByCashSessionId(UUID cashSessionId);
}
