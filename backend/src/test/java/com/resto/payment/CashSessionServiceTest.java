package com.resto.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resto.audit.service.AuditService;
import com.resto.order.domain.Order;
import com.resto.order.domain.OrderItem;
import com.resto.order.repository.OrderRepository;
import com.resto.payment.domain.CashSession;
import com.resto.payment.domain.Payment;
import com.resto.payment.dto.CashSessionDto;
import com.resto.payment.dto.CashSessionReportDto;
import com.resto.payment.repository.CashSessionRepository;
import com.resto.payment.repository.PaymentRepository;
import com.resto.payment.service.CashSessionService;
import com.resto.tenant.domain.Store;
import com.resto.tenant.domain.User;
import com.resto.tenant.repository.StoreRepository;
import com.resto.tenant.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashSessionServiceTest {

    @Mock CashSessionRepository cashSessionRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock OrderRepository orderRepository;
    @Mock StoreRepository storeRepository;
    @Mock UserRepository userRepository;
    @Mock AuditService auditService;

    CashSessionService service;

    UUID orgId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();
    UUID cashierId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CashSessionService(
                cashSessionRepository, paymentRepository, orderRepository,
                storeRepository, userRepository, auditService, new ObjectMapper());
    }

    @Test
    @DisplayName("Open creates one OPEN session per cashier/store")
    void openSession() {
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store()));
        when(cashSessionRepository.findByStoreIdAndOpenedByUserIdAndStatus(storeId, cashierId, "OPEN"))
                .thenReturn(Optional.empty());
        when(cashSessionRepository.save(any(CashSession.class))).thenAnswer(inv -> {
            CashSession s = inv.getArgument(0);
            s.setId(sessionId);
            return s;
        });
        when(userRepository.findById(cashierId)).thenReturn(Optional.of(user()));

        CashSessionDto dto = service.open(orgId, storeId, cashierId, new BigDecimal("50.00"));

        assertEquals(sessionId, dto.getId());
        assertEquals("OPEN", dto.getStatus());
        assertEquals(new BigDecimal("50.00"), dto.getOpeningFloat());
        verify(auditService).record(eq(orgId), eq(storeId), eq(cashierId), eq("CASH_SESSION_OPENED"), any(), eq(sessionId), any());
    }

    @Test
    @DisplayName("Open rejects second OPEN session for same cashier")
    void openRejectsDuplicate() {
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store()));
        when(cashSessionRepository.findByStoreIdAndOpenedByUserIdAndStatus(storeId, cashierId, "OPEN"))
                .thenReturn(Optional.of(CashSession.builder().id(sessionId).status("OPEN").build()));

        assertThrows(IllegalStateException.class,
                () -> service.open(orgId, storeId, cashierId, null));
    }

    @Test
    @DisplayName("Close builds report with CA, products and order history")
    void closeBuildsReport() {
        CashSession session = CashSession.builder()
                .id(sessionId)
                .organizationId(orgId)
                .storeId(storeId)
                .openedByUserId(cashierId)
                .status("OPEN")
                .openedAt(OffsetDateTime.now().minusHours(2))
                .openingFloat(new BigDecimal("20.00"))
                .build();
        when(cashSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store()));
        when(userRepository.findById(cashierId)).thenReturn(Optional.of(user()));

        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .cashSessionId(sessionId)
                .paymentMethod("CASH")
                .amount(new BigDecimal("15.00"))
                .build();
        when(paymentRepository.findByCashSessionId(sessionId)).thenReturn(List.of(payment));

        OrderItem item = OrderItem.builder()
                .productId(productId)
                .productName("Burger")
                .quantity(2)
                .subtotal(new BigDecimal("15.00"))
                .build();
        Order order = Order.builder()
                .id(orderId)
                .organizationId(orgId)
                .storeId(storeId)
                .orderNumber(101)
                .orderType("DINE_IN")
                .status("CLOSED")
                .paymentStatus("PAID")
                .totalAmount(new BigDecimal("15.00"))
                .createdBy(cashierId)
                .createdAt(OffsetDateTime.now().minusHours(1))
                .items(List.of(item))
                .build();
        when(orderRepository.findAllById(any())).thenReturn(List.of(order));
        when(cashSessionRepository.save(any(CashSession.class))).thenAnswer(i -> i.getArgument(0));

        CashSessionDto closed = service.close(sessionId, cashierId, "Fin de service");

        assertEquals("CLOSED", closed.getStatus());
        assertNotNull(closed.getReport());
        CashSessionReportDto report = closed.getReport();
        assertEquals(0, new BigDecimal("15.00").compareTo(report.getTotalRevenue()));
        assertEquals(1, report.getPaymentCount());
        assertEquals(1, report.getProducts().size());
        assertEquals("Burger", report.getProducts().get(0).getProductName());
        assertEquals(2, report.getProducts().get(0).getQuantity());
        assertEquals(1, report.getOrders().size());
        assertEquals(101, report.getOrders().get(0).getOrderNumber());
        assertNotNull(session.getReportSnapshot());
    }

    private Store store() {
        Store store = new Store();
        store.setId(storeId);
        store.setName("Resto Centre");
        return store;
    }

    private User user() {
        User user = new User();
        user.setId(cashierId);
        user.setFirstName("Alice");
        user.setLastName("Cash");
        user.setEmail("alice@resto.test");
        return user;
    }
}
