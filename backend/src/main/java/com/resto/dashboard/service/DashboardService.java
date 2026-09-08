package com.resto.dashboard.service;

import com.resto.dashboard.dto.DashboardMetricsDto;
import com.resto.floorplan.domain.RestaurantTable;
import com.resto.floorplan.repository.RestaurantTableRepository;
import com.resto.order.domain.Order;
import com.resto.order.repository.OrderRepository;
import com.resto.payment.domain.Payment;
import com.resto.payment.repository.PaymentRepository;
import com.resto.tenant.domain.Store;
import com.resto.tenant.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RestaurantTableRepository tableRepository;
    private final StoreRepository storeRepository;

    public DashboardService(OrderRepository orderRepository,
                            PaymentRepository paymentRepository,
                            RestaurantTableRepository tableRepository,
                            StoreRepository storeRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.tableRepository = tableRepository;
        this.storeRepository = storeRepository;
    }

    public DashboardMetricsDto getStoreMetrics(UUID storeId) {
        Store store = storeRepository.findById(storeId).orElse(null);
        ZoneId zone = ZoneId.of(store != null && store.getTimezone() != null ? store.getTimezone() : "UTC");
        ZonedDateTime startOfDayZoned = LocalDate.now(zone).atStartOfDay(zone);
        OffsetDateTime startOfDay = startOfDayZoned.toOffsetDateTime();

        List<Order> todaysOrders = orderRepository.findByStoreIdAndCreatedAtGreaterThanEqual(storeId, startOfDay);
        List<Order> allOrders = orderRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
        List<Payment> payments = paymentRepository.findByStoreId(storeId);
        List<RestaurantTable> tables = tableRepository.findByStoreId(storeId);

        long todaysOrdersCount = todaysOrders.size();

        BigDecimal declaredRevenue = payments.stream()
                .filter(p -> p.getCreatedAt() != null && !p.getCreatedAt().isBefore(startOfDay))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long unpaidOrdersCount = allOrders.stream()
                .filter(o -> "UNPAID".equalsIgnoreCase(o.getPaymentStatus()))
                .filter(o -> !"CANCELLED".equalsIgnoreCase(o.getStatus()) && !"CLOSED".equalsIgnoreCase(o.getStatus()))
                .count();

        long preparingOrdersCount = allOrders.stream()
                .filter(o -> "PREPARING".equalsIgnoreCase(o.getStatus()))
                .count();

        long readyOrdersCount = allOrders.stream()
                .filter(o -> "READY".equalsIgnoreCase(o.getStatus()))
                .count();

        long occupiedTablesCount = tables.stream()
                .filter(t -> "OCCUPIED".equalsIgnoreCase(t.getStatus()))
                .count();

        return DashboardMetricsDto.builder()
                .todaysOrdersCount(todaysOrdersCount)
                .declaredRevenue(declaredRevenue)
                .unpaidOrdersCount(unpaidOrdersCount)
                .preparingOrdersCount(preparingOrdersCount)
                .readyOrdersCount(readyOrdersCount)
                .occupiedTablesCount(occupiedTablesCount)
                .build();
    }
}
