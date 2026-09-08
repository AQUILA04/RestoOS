package com.resto.dashboard.service;

import com.resto.dashboard.dto.DashboardMetricsDto;
import com.resto.floorplan.domain.RestaurantTable;
import com.resto.floorplan.repository.RestaurantTableRepository;
import com.resto.order.domain.Order;
import com.resto.order.repository.OrderRepository;
import com.resto.payment.domain.Payment;
import com.resto.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RestaurantTableRepository tableRepository;

    public DashboardService(OrderRepository orderRepository,
                            PaymentRepository paymentRepository,
                            RestaurantTableRepository tableRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.tableRepository = tableRepository;
    }

    public DashboardMetricsDto getStoreMetrics(UUID storeId) {
        List<Order> orders = orderRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
        List<Payment> payments = paymentRepository.findByStoreId(storeId);
        List<RestaurantTable> tables = tableRepository.findByStoreId(storeId);

        long todaysOrdersCount = orders.size();
        BigDecimal declaredRevenue = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long unpaidOrdersCount = orders.stream()
                .filter(o -> "UNPAID".equalsIgnoreCase(o.getPaymentStatus()))
                .count();

        long preparingOrdersCount = orders.stream()
                .filter(o -> "PREPARING".equalsIgnoreCase(o.getStatus()))
                .count();

        long readyOrdersCount = orders.stream()
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
