package com.resto.payment;

import com.resto.audit.service.AuditService;
import com.resto.order.domain.Order;
import com.resto.order.repository.OrderRepository;
import com.resto.order.service.OrderService;
import com.resto.payment.domain.CashSession;
import com.resto.payment.domain.Payment;
import com.resto.payment.repository.CashSessionRepository;
import com.resto.payment.repository.PaymentRepository;
import com.resto.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock OrderRepository orderRepository;
    @Mock CashSessionRepository cashSessionRepository;
    @Mock AuditService auditService;
    @Mock OrderService orderService;

    PaymentService paymentService;

    UUID orgId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    UUID cashierId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository, orderRepository, cashSessionRepository, auditService, orderService);
        CashSession session = CashSession.builder()
                .id(sessionId)
                .organizationId(orgId)
                .storeId(storeId)
                .openedByUserId(cashierId)
                .status("OPEN")
                .build();
        lenient().when(cashSessionRepository.findByStoreIdAndOpenedByUserIdAndStatus(storeId, cashierId, "OPEN"))
                .thenReturn(Optional.of(session));
    }

    @Test
    @DisplayName("Full payment marks PAID and CLOSED")
    void fullPaymentCloses() {
        Order order = Order.builder()
                .id(orderId)
                .organizationId(orgId)
                .storeId(storeId)
                .orderNumber(1)
                .status("DELIVERED")
                .paymentStatus("UNPAID")
                .totalAmount(new BigDecimal("20.00"))
                .build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(paymentRepository.findByOrderId(orderId)).thenAnswer(inv -> List.of(
                Payment.builder().amount(new BigDecimal("20.00")).orderId(orderId).cashSessionId(sessionId).build()
        ));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Payment payment = paymentService.markPaid(orgId, storeId, orderId, cashierId, "CARD", new BigDecimal("20.00"));

        assertNotNull(payment);
        assertEquals(sessionId, payment.getCashSessionId());
        assertEquals("PAID", order.getPaymentStatus());
        assertEquals("CLOSED", order.getStatus());
        verify(auditService).record(eq(orgId), eq(storeId), eq(cashierId), eq("PAYMENT_RECORDED"), any(), eq(orderId), any());
    }

    @Test
    @DisplayName("Partial payment stays UNPAID")
    void partialStaysUnpaid() {
        Order order = Order.builder()
                .id(orderId)
                .organizationId(orgId)
                .storeId(storeId)
                .orderNumber(1)
                .status("DELIVERED")
                .paymentStatus("UNPAID")
                .totalAmount(new BigDecimal("20.00"))
                .build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(List.of(
                Payment.builder().amount(new BigDecimal("5.00")).build()
        ));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        paymentService.markPaid(orgId, storeId, orderId, cashierId, "CASH", new BigDecimal("5.00"));
        assertEquals("UNPAID", order.getPaymentStatus());
        assertEquals("DELIVERED", order.getStatus());
    }

    @Test
    @DisplayName("Early full payment marks PAID but keeps kitchen status")
    void earlyPaymentKeepsOperationalStatus() {
        Order order = Order.builder()
                .id(orderId)
                .organizationId(orgId)
                .storeId(storeId)
                .orderNumber(1)
                .status("SENT_TO_KITCHEN")
                .paymentStatus("UNPAID")
                .totalAmount(new BigDecimal("20.00"))
                .build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(paymentRepository.findByOrderId(orderId)).thenAnswer(inv -> List.of(
                Payment.builder().amount(new BigDecimal("20.00")).orderId(orderId).build()
        ));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        paymentService.markPaid(orgId, storeId, orderId, cashierId, "CARD", new BigDecimal("20.00"));

        assertEquals("PAID", order.getPaymentStatus());
        assertEquals("SENT_TO_KITCHEN", order.getStatus());
        verify(orderService, never()).releaseTableIfIdle(any(), any());
    }

    @Test
    @DisplayName("Rejects payment without open cash session")
    void rejectsWithoutOpenSession() {
        when(cashSessionRepository.findByStoreIdAndOpenedByUserIdAndStatus(storeId, cashierId, "OPEN"))
                .thenReturn(Optional.empty());
        Order order = Order.builder()
                .id(orderId).organizationId(orgId).storeId(storeId).orderNumber(1)
                .status("READY").totalAmount(new BigDecimal("10.00")).build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class,
                () -> paymentService.markPaid(orgId, storeId, orderId, cashierId, "CASH", new BigDecimal("10.00")));
    }

    @Test
    @DisplayName("Rejects cancelled order and invalid method")
    void rejectsInvalid() {
        Order cancelled = Order.builder()
                .id(orderId).organizationId(orgId).storeId(storeId).orderNumber(1)
                .status("CANCELLED").totalAmount(new BigDecimal("10.00")).build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(cancelled));
        assertThrows(IllegalStateException.class,
                () -> paymentService.markPaid(orgId, storeId, orderId, cashierId, "CASH", new BigDecimal("10.00")));

        Order open = Order.builder()
                .id(orderId).organizationId(orgId).storeId(storeId).orderNumber(1)
                .status("READY").totalAmount(new BigDecimal("10.00")).build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(open));
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.markPaid(orgId, storeId, orderId, cashierId, "BITCOIN", new BigDecimal("10.00")));
    }
}
