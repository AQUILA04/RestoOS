package com.resto.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.resto.audit.service.AuditService;
import com.resto.core.security.JwtAuth;
import com.resto.order.domain.Order;
import com.resto.order.domain.OrderItem;
import com.resto.order.repository.OrderRepository;
import com.resto.payment.domain.CashSession;
import com.resto.payment.domain.Payment;
import com.resto.payment.dto.CashSessionDto;
import com.resto.payment.dto.CashSessionReportDto;
import com.resto.payment.repository.CashSessionRepository;
import com.resto.payment.repository.PaymentRepository;
import com.resto.tenant.domain.Store;
import com.resto.tenant.domain.User;
import com.resto.tenant.repository.StoreRepository;
import com.resto.tenant.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CashSessionService {

    private final CashSessionRepository cashSessionRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public CashSessionService(CashSessionRepository cashSessionRepository,
                              PaymentRepository paymentRepository,
                              OrderRepository orderRepository,
                              StoreRepository storeRepository,
                              UserRepository userRepository,
                              AuditService auditService,
                              ObjectMapper objectMapper) {
        this.cashSessionRepository = cashSessionRepository;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper.copy()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public CashSessionDto open(UUID organizationId, UUID storeId, UUID userId, BigDecimal openingFloat) {
        if (organizationId == null || storeId == null || userId == null) {
            throw new IllegalArgumentException("organizationId, storeId and userId are required");
        }
        storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));

        if (cashSessionRepository.findByStoreIdAndOpenedByUserIdAndStatus(storeId, userId, "OPEN").isPresent()) {
            throw new IllegalStateException("Cash session already open for this cashier on this store");
        }

        CashSession session = CashSession.builder()
                .organizationId(organizationId)
                .storeId(storeId)
                .openedByUserId(userId)
                .status("OPEN")
                .openedAt(OffsetDateTime.now())
                .openingFloat(openingFloat)
                .build();
        CashSession saved = cashSessionRepository.save(session);

        auditService.record(organizationId, storeId, userId,
                "CASH_SESSION_OPENED", "CASH_SESSION", saved.getId(),
                "openingFloat=" + openingFloat);

        return toDto(saved, false);
    }

    @Transactional(readOnly = true)
    public Optional<CashSessionDto> getCurrent(UUID storeId, UUID userId) {
        return cashSessionRepository
                .findByStoreIdAndOpenedByUserIdAndStatus(storeId, userId, "OPEN")
                .map(s -> toDto(s, true));
    }

    @Transactional(readOnly = true)
    public CashSessionDto getById(UUID sessionId) {
        return toDto(requireSession(sessionId), true);
    }

    @Transactional(readOnly = true)
    public List<CashSessionDto> list(UUID storeId, UUID cashierUserId) {
        List<CashSession> sessions = cashierUserId != null
                ? cashSessionRepository.findByStoreIdAndOpenedByUserIdOrderByOpenedAtDesc(storeId, cashierUserId)
                : cashSessionRepository.findByStoreIdOrderByOpenedAtDesc(storeId);
        return sessions.stream().map(s -> toDto(s, false)).collect(Collectors.toList());
    }

    public CashSessionDto close(UUID sessionId, UUID actorUserId, String closingNotes) {
        CashSession session = requireSession(sessionId);
        if (!"OPEN".equals(session.getStatus())) {
            throw new IllegalStateException("Cash session is already closed");
        }
        assertCanClose(session, actorUserId);

        CashSessionReportDto report = buildReport(session);
        try {
            session.setReportSnapshot(objectMapper.writeValueAsString(report));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize cash session report", e);
        }
        session.setStatus("CLOSED");
        session.setClosedAt(OffsetDateTime.now());
        session.setClosedByUserId(actorUserId);
        session.setClosingNotes(closingNotes);
        CashSession saved = cashSessionRepository.save(session);

        auditService.record(session.getOrganizationId(), session.getStoreId(), actorUserId,
                "CASH_SESSION_CLOSED", "CASH_SESSION", saved.getId(),
                "totalRevenue=" + report.getTotalRevenue());

        CashSessionDto dto = toDto(saved, false);
        dto.setReport(report);
        return dto;
    }

    @Transactional(readOnly = true)
    public CashSessionReportDto getReport(UUID sessionId) {
        CashSession session = requireSession(sessionId);
        if ("CLOSED".equals(session.getStatus()) && session.getReportSnapshot() != null) {
            try {
                return objectMapper.readValue(session.getReportSnapshot(), CashSessionReportDto.class);
            } catch (JsonProcessingException ignored) {
                // Fall through to live rebuild
            }
        }
        return buildReport(session);
    }

    @Transactional(readOnly = true)
    public Optional<CashSession> findOpenSession(UUID storeId, UUID cashierUserId) {
        return cashSessionRepository.findByStoreIdAndOpenedByUserIdAndStatus(storeId, cashierUserId, "OPEN");
    }

    public CashSessionReportDto buildReport(CashSession session) {
        List<Payment> payments = paymentRepository.findByCashSessionId(session.getId());
        Store store = storeRepository.findById(session.getStoreId()).orElse(null);

        CashSessionReportDto report = new CashSessionReportDto();
        report.setSessionId(session.getId());
        report.setStoreId(session.getStoreId());
        report.setStoreName(store != null ? store.getName() : null);
        report.setCashierUserId(session.getOpenedByUserId());
        report.setCashierName(resolveUserName(session.getOpenedByUserId()));
        report.setStatus(session.getStatus());
        report.setOpenedAt(session.getOpenedAt());
        report.setClosedAt(session.getClosedAt());
        report.setOpeningFloat(session.getOpeningFloat());
        report.setClosingNotes(session.getClosingNotes());
        report.setPaymentCount(payments.size());

        BigDecimal total = BigDecimal.ZERO;
        Map<String, BigDecimal> byMethod = new LinkedHashMap<>();
        Map<UUID, List<Payment>> paymentsByOrder = new LinkedHashMap<>();

        for (Payment payment : payments) {
            BigDecimal amount = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
            total = total.add(amount);
            String method = payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "OTHER";
            byMethod.merge(method, amount, BigDecimal::add);
            paymentsByOrder.computeIfAbsent(payment.getOrderId(), k -> new ArrayList<>()).add(payment);
        }
        report.setTotalRevenue(total);
        report.setRevenueByMethod(byMethod);

        Set<UUID> orderIds = new LinkedHashSet<>(paymentsByOrder.keySet());
        Map<UUID, Order> orders = orderIds.isEmpty()
                ? Map.of()
                : orderRepository.findAllById(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, o -> o, (a, b) -> a, LinkedHashMap::new));

        for (Order order : orders.values()) {
            if (order.getItems() != null) {
                order.getItems().size();
            }
        }

        Map<String, CashSessionReportDto.ProductLine> productMap = new HashMap<>();
        List<CashSessionReportDto.OrderLine> orderLines = new ArrayList<>();

        for (UUID orderId : orderIds) {
            Order order = orders.get(orderId);
            List<Payment> orderPayments = paymentsByOrder.getOrDefault(orderId, List.of());
            BigDecimal sessionPaid = orderPayments.stream()
                    .map(Payment::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            CashSessionReportDto.OrderLine line = new CashSessionReportDto.OrderLine();
            if (order != null) {
                line.setOrderId(order.getId());
                line.setOrderNumber(order.getOrderNumber());
                line.setCreatedAt(order.getCreatedAt());
                line.setOrderType(order.getOrderType());
                line.setStatus(order.getStatus());
                line.setPaymentStatus(order.getPaymentStatus());
                line.setOrderTotal(order.getTotalAmount());
                line.setCreatedByUserId(order.getCreatedBy());
                line.setCreatedByName(resolveUserName(order.getCreatedBy()));

                if (order.getItems() != null) {
                    for (OrderItem item : order.getItems()) {
                        String key = item.getProductId() != null
                                ? item.getProductId().toString()
                                : (item.getProductName() != null ? item.getProductName() : "unknown");
                        CashSessionReportDto.ProductLine product = productMap.computeIfAbsent(key, k -> {
                            CashSessionReportDto.ProductLine pl = new CashSessionReportDto.ProductLine();
                            pl.setProductId(item.getProductId());
                            pl.setProductName(item.getProductName() != null ? item.getProductName() : "Produit");
                            pl.setQuantity(0);
                            pl.setTotalAmount(BigDecimal.ZERO);
                            return pl;
                        });
                        int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                        BigDecimal amount = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
                        product.setQuantity(product.getQuantity() + qty);
                        product.setTotalAmount(product.getTotalAmount().add(amount));
                    }
                }
            } else {
                line.setOrderId(orderId);
            }
            line.setSessionPaidAmount(sessionPaid);
            line.setPaymentMethods(orderPayments.stream()
                    .map(Payment::getPaymentMethod)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList()));
            orderLines.add(line);
        }

        orderLines.sort(Comparator.comparing(
                CashSessionReportDto.OrderLine::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));

        List<CashSessionReportDto.ProductLine> products = new ArrayList<>(productMap.values());
        products.sort(Comparator.comparing(
                CashSessionReportDto.ProductLine::getProductName,
                Comparator.nullsLast(String::compareToIgnoreCase)));

        report.setProducts(products);
        report.setOrders(orderLines);
        return report;
    }

    private void assertCanClose(CashSession session, UUID actorUserId) {
        if (actorUserId != null && actorUserId.equals(session.getOpenedByUserId())) {
            return;
        }
        if (JwtAuth.hasAnyRole("OWNER", "ADMIN", "STORE_MANAGER")) {
            return;
        }
        throw new IllegalStateException("Only the session cashier or a manager can close this cash session");
    }

    private CashSession requireSession(UUID sessionId) {
        return cashSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Cash session not found: " + sessionId));
    }

    private CashSessionDto toDto(CashSession session, boolean includeLiveReport) {
        CashSessionDto dto = new CashSessionDto();
        dto.setId(session.getId());
        dto.setOrganizationId(session.getOrganizationId());
        dto.setStoreId(session.getStoreId());
        storeRepository.findById(session.getStoreId()).ifPresent(s -> dto.setStoreName(s.getName()));
        dto.setOpenedByUserId(session.getOpenedByUserId());
        dto.setOpenedByName(resolveUserName(session.getOpenedByUserId()));
        dto.setClosedByUserId(session.getClosedByUserId());
        if (session.getClosedByUserId() != null) {
            dto.setClosedByName(resolveUserName(session.getClosedByUserId()));
        }
        dto.setStatus(session.getStatus());
        dto.setOpenedAt(session.getOpenedAt());
        dto.setClosedAt(session.getClosedAt());
        dto.setOpeningFloat(session.getOpeningFloat());
        dto.setClosingNotes(session.getClosingNotes());

        if ("CLOSED".equals(session.getStatus()) && session.getReportSnapshot() != null) {
            try {
                dto.setReport(objectMapper.readValue(session.getReportSnapshot(), CashSessionReportDto.class));
            } catch (JsonProcessingException ignored) {
                if (includeLiveReport) {
                    dto.setReport(buildReport(session));
                }
            }
        } else if (includeLiveReport) {
            dto.setReport(buildReport(session));
        }
        return dto;
    }

    private String resolveUserName(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).map(this::formatUserName).orElse("Caissier");
    }

    private String formatUserName(User user) {
        String display = ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();
        if (display.isBlank()) {
            return user.getEmail() != null ? user.getEmail() : "Caissier";
        }
        return display;
    }
}
