package com.resto.payment.service;

import com.resto.audit.domain.AuditLog;
import com.resto.audit.repository.AuditLogRepository;
import com.resto.order.domain.Order;
import com.resto.order.repository.OrderRepository;
import com.resto.payment.domain.Payment;
import com.resto.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final AuditLogRepository auditLogRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          AuditLogRepository auditLogRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public Payment recordPayment(UUID organizationId, UUID storeId, UUID orderId, UUID cashierUserId, String paymentMethod, BigDecimal amount) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        if ("CANCELLED".equals(order.getStatus())) {
            throw new IllegalStateException("Cannot record payment for CANCELLED order: " + orderId);
        }

        Payment payment = Payment.builder()
                .organizationId(organizationId)
                .storeId(storeId)
                .orderId(orderId)
                .cashierUserId(cashierUserId)
                .paymentMethod(paymentMethod)
                .amount(amount)
                .build();
        Payment savedPayment = paymentRepository.save(payment);

        BigDecimal totalPaid = paymentRepository.findByOrderId(orderId).stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (order.getTotalAmount() == null || totalPaid.compareTo(order.getTotalAmount()) >= 0) {
            order.setPaymentStatus("PAID");
        } else {
            order.setPaymentStatus("PARTIALLY_PAID");
        }

        if ("READY".equals(order.getStatus())) {
            order.setStatus("DELIVERED");
        }
        orderRepository.save(order);

        // Audit Log Entry
        AuditLog auditLog = AuditLog.builder()
                .organizationId(organizationId)
                .storeId(storeId)
                .userId(cashierUserId)
                .action("PAYMENT_RECORDED")
                .entityType("ORDER")
                .entityId(orderId)
                .details("Recorded declarative payment: " + paymentMethod + " (" + amount + " EUR)")
                .build();
        auditLogRepository.save(auditLog);

        return savedPayment;
    }
}
