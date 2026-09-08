package com.resto.payment.service;

import com.resto.audit.service.AuditService;
import com.resto.order.domain.Order;
import com.resto.order.domain.OrderStateMachine;
import com.resto.order.repository.OrderRepository;
import com.resto.order.service.OrderService;
import com.resto.payment.domain.Payment;
import com.resto.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private static final Set<String> METHODS = Set.of("CASH", "CARD", "MOBILE_MONEY", "OTHER");

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final AuditService auditService;
    private final OrderService orderService;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          AuditService auditService,
                          OrderService orderService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.auditService = auditService;
        this.orderService = orderService;
    }

    /**
     * Mark order paid (declarative). When amount covers total → PAID + CLOSED.
     * Invariant: CLOSED + UNPAID is forbidden.
     */
    public Payment markPaid(UUID organizationId, UUID storeId, UUID orderId, UUID cashierUserId,
                            String paymentMethod, BigDecimal amount) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!organizationId.equals(order.getOrganizationId())) {
            throw new IllegalArgumentException("Order organization mismatch");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        if ("CANCELLED".equals(order.getStatus()) || "CLOSED".equals(order.getStatus())) {
            throw new IllegalStateException("Cannot record payment for order in status: " + order.getStatus());
        }
        if (paymentMethod == null || !METHODS.contains(paymentMethod)) {
            throw new IllegalArgumentException("Invalid payment method: " + paymentMethod);
        }

        Payment payment = Payment.builder()
                .organizationId(organizationId)
                .storeId(storeId != null ? storeId : order.getStoreId())
                .orderId(orderId)
                .cashierUserId(cashierUserId)
                .paymentMethod(paymentMethod)
                .amount(amount)
                .build();
        Payment savedPayment = paymentRepository.save(payment);

        BigDecimal totalPaid = paymentRepository.findByOrderId(orderId).stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (order.getTotalAmount() != null && totalPaid.compareTo(order.getTotalAmount()) >= 0) {
            order.setPaymentStatus("PAID");
            // Fully paid → close (from DELIVERED or READY/earlier operational states after payment)
            if ("DELIVERED".equals(order.getStatus()) || "READY".equals(order.getStatus())
                    || "PREPARING".equals(order.getStatus()) || "SENT_TO_KITCHEN".equals(order.getStatus())
                    || "CREATED".equals(order.getStatus())) {
                // Prefer CLOSED only when paid; skip invalid machine edges by setting directly when paid
                order.setStatus("CLOSED");
            }
            if (order.getTableId() != null) {
                orderService.releaseTableIfIdle(order.getStoreId(), order.getTableId());
            }
        } else {
            // Partial payments stay UNPAID until fully covered (contract: UNPAID | PAID only)
            order.setPaymentStatus("UNPAID");
        }

        // Guard invariant
        if ("CLOSED".equals(order.getStatus()) && !"PAID".equals(order.getPaymentStatus())) {
            throw new IllegalStateException("CLOSED + UNPAID is forbidden");
        }

        orderRepository.save(order);

        auditService.record(organizationId, order.getStoreId(), cashierUserId,
                "PAYMENT_RECORDED", "ORDER", orderId,
                "method=" + paymentMethod + " amount=" + amount);

        return savedPayment;
    }

    /** @deprecated use {@link #markPaid} */
    public Payment recordPayment(UUID organizationId, UUID storeId, UUID orderId, UUID cashierUserId,
                                 String paymentMethod, BigDecimal amount) {
        return markPaid(organizationId, storeId, orderId, cashierUserId, paymentMethod, amount);
    }
}
