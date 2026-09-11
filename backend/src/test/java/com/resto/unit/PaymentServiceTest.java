package com.resto.unit;

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
import org.mockito.ArgumentCaptor;
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

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository, orderRepository, cashSessionRepository, auditService, orderService);
    }

    @Test
    @DisplayName("Full payment marks PAID and CLOSED and releases table")
    void fullPaymentCloses() {
        UUID org = UUID.randomUUID();
        UUID store = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();
        UUID cashier = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        Order order = Order.builder()
                .id(orderId)
                .organizationId(org)
                .storeId(store)
                .tableId(tableId)
                .status("DELIVERED")
                .paymentStatus("UNPAID")
                .totalAmount(new BigDecimal("15.50"))
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(cashSessionRepository.findByStoreIdAndOpenedByUserIdAndStatus(store, cashier, "OPEN"))
                .thenReturn(Optional.of(CashSession.builder().id(sessionId).status("OPEN").build()));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByOrderId(orderId)).thenAnswer(inv -> {
            Payment p = Payment.builder().amount(new BigDecimal("15.50")).cashSessionId(sessionId).build();
            return List.of(p);
        });

        paymentService.markPaid(org, store, orderId, cashier, "CASH", new BigDecimal("15.50"));

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        assertEquals("PAID", saved.getValue().getPaymentStatus());
        assertEquals("CLOSED", saved.getValue().getStatus());
        verify(orderService).releaseTableIfIdle(store, tableId);
        verify(auditService).record(eq(org), eq(store), eq(cashier), eq("PAYMENT_RECORDED"), eq("ORDER"), eq(orderId), anyString());
    }

    @Test
    @DisplayName("Rejects payment on cancelled order")
    void rejectsCancelled() {
        UUID org = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .organizationId(org)
                .storeId(UUID.randomUUID())
                .status("CANCELLED")
                .paymentStatus("UNPAID")
                .totalAmount(new BigDecimal("10.00"))
                .build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        assertThrows(IllegalStateException.class,
                () -> paymentService.markPaid(org, order.getStoreId(), orderId, UUID.randomUUID(), "CASH", new BigDecimal("10.00")));
    }
}
