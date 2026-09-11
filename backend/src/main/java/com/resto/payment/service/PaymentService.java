package com.resto.payment.service;

import com.resto.audit.service.AuditService;
import com.resto.order.domain.Order;
import com.resto.order.repository.OrderRepository;
import com.resto.order.service.OrderService;
import com.resto.payment.domain.CashSession;
import com.resto.payment.domain.Payment;
import com.resto.payment.repository.CashSessionRepository;
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
    private final CashSessionRepository cashSessionRepository;
    private final AuditService auditService;
    private final OrderService orderService;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          CashSessionRepository cashSessionRepository,
                          AuditService auditService,
                          OrderService orderService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.cashSessionRepository = cashSessionRepository;
        this.auditService = auditService;
        this.orderService = orderService;
    }

    public Payment markPaid(UUID organizationId, UUID storeId, UUID orderId, UUID cashierUserId,
                            String paymentMethod, BigDecimal amount) {
        return markPaid(organizationId, storeId, orderId, cashierUserId, paymentMethod, amount, null);
    }

    public Payment markPaid(UUID organizationId, UUID storeId, UUID orderId, UUID cashierUserId,
                            String paymentMethod, BigDecimal amount, BigDecimal amountTendered) {
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

        UUID effectiveStoreId = storeId != null ? storeId : order.getStoreId();
        CashSession openSession = cashSessionRepository
                .findByStoreIdAndOpenedByUserIdAndStatus(effectiveStoreId, cashierUserId, "OPEN")
                .orElseThrow(() -> new IllegalStateException(
                        "No open cash session for this cashier. Open the register before recording payments."));

        BigDecimal changeAmount = null;
        if ("CASH".equals(paymentMethod) && amountTendered != null) {
            if (amountTendered.compareTo(amount) < 0) {
                throw new IllegalArgumentException("Cash received must cover payment amount");
            }
            changeAmount = amountTendered.subtract(amount);
        }

        Payment payment = Payment.builder()
                .organizationId(organizationId)
                .storeId(effectiveStoreId)
                .orderId(orderId)
                .cashierUserId(cashierUserId)
                .cashSessionId(openSession.getId())
                .paymentMethod(paymentMethod)
                .amount(amount)
                .amountTendered(amountTendered)
                .changeAmount(changeAmount)
                .build();
        Payment savedPayment = paymentRepository.save(payment);

        BigDecimal totalPaid = paymentRepository.findByOrderId(orderId).stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (order.getTotalAmount() != null && totalPaid.compareTo(order.getTotalAmount()) >= 0) {
            order.setPaymentStatus("PAID");
            if ("DELIVERED".equals(order.getStatus())) {
                order.setStatus("CLOSED");
                if (order.getTableId() != null) {
                    orderService.releaseTableIfIdle(order.getStoreId(), order.getTableId());
                }
            }
        } else {
            order.setPaymentStatus("UNPAID");
        }

        if ("CLOSED".equals(order.getStatus()) && !"PAID".equals(order.getPaymentStatus())) {
            throw new IllegalStateException("CLOSED + UNPAID is forbidden");
        }

        orderRepository.save(order);

        auditService.record(organizationId, order.getStoreId(), cashierUserId,
                "PAYMENT_RECORDED", "ORDER", orderId,
                "method=" + paymentMethod + " amount=" + amount + " cashSessionId=" + openSession.getId());

        return savedPayment;
    }

    @Deprecated
    public Payment recordPayment(UUID organizationId, UUID storeId, UUID orderId, UUID cashierUserId,
                                 String paymentMethod, BigDecimal amount) {
        return markPaid(organizationId, storeId, orderId, cashierUserId, paymentMethod, amount);
    }
}
