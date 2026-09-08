package com.resto.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsDto {
    private long todaysOrdersCount;
    private BigDecimal declaredRevenue;
    private long unpaidOrdersCount;
    private long preparingOrdersCount;
    private long readyOrdersCount;
    private long occupiedTablesCount;

    public long getTodaysOrdersCount() { return todaysOrdersCount; }
    public void setTodaysOrdersCount(long todaysOrdersCount) { this.todaysOrdersCount = todaysOrdersCount; }
    public BigDecimal getDeclaredRevenue() { return declaredRevenue; }
    public void setDeclaredRevenue(BigDecimal declaredRevenue) { this.declaredRevenue = declaredRevenue; }
    public long getUnpaidOrdersCount() { return unpaidOrdersCount; }
    public void setUnpaidOrdersCount(long unpaidOrdersCount) { this.unpaidOrdersCount = unpaidOrdersCount; }
    public long getPreparingOrdersCount() { return preparingOrdersCount; }
    public void setPreparingOrdersCount(long preparingOrdersCount) { this.preparingOrdersCount = preparingOrdersCount; }
    public long getReadyOrdersCount() { return readyOrdersCount; }
    public void setReadyOrdersCount(long readyOrdersCount) { this.readyOrdersCount = readyOrdersCount; }
    public long getOccupiedTablesCount() { return occupiedTablesCount; }
    public void setOccupiedTablesCount(long occupiedTablesCount) { this.occupiedTablesCount = occupiedTablesCount; }

    public static DashboardMetricsDtoBuilder builder() { return new DashboardMetricsDtoBuilder(); }

    public static class DashboardMetricsDtoBuilder {
        private long todaysOrdersCount;
        private BigDecimal declaredRevenue = BigDecimal.ZERO;
        private long unpaidOrdersCount;
        private long preparingOrdersCount;
        private long readyOrdersCount;
        private long occupiedTablesCount;

        public DashboardMetricsDtoBuilder todaysOrdersCount(long todaysOrdersCount) { this.todaysOrdersCount = todaysOrdersCount; return this; }
        public DashboardMetricsDtoBuilder declaredRevenue(BigDecimal declaredRevenue) { this.declaredRevenue = declaredRevenue; return this; }
        public DashboardMetricsDtoBuilder unpaidOrdersCount(long unpaidOrdersCount) { this.unpaidOrdersCount = unpaidOrdersCount; return this; }
        public DashboardMetricsDtoBuilder preparingOrdersCount(long preparingOrdersCount) { this.preparingOrdersCount = preparingOrdersCount; return this; }
        public DashboardMetricsDtoBuilder readyOrdersCount(long readyOrdersCount) { this.readyOrdersCount = readyOrdersCount; return this; }
        public DashboardMetricsDtoBuilder occupiedTablesCount(long occupiedTablesCount) { this.occupiedTablesCount = occupiedTablesCount; return this; }

        public DashboardMetricsDto build() {
            DashboardMetricsDto dto = new DashboardMetricsDto();
            dto.setTodaysOrdersCount(this.todaysOrdersCount);
            dto.setDeclaredRevenue(this.declaredRevenue != null ? this.declaredRevenue : BigDecimal.ZERO);
            dto.setUnpaidOrdersCount(this.unpaidOrdersCount);
            dto.setPreparingOrdersCount(this.preparingOrdersCount);
            dto.setReadyOrdersCount(this.readyOrdersCount);
            dto.setOccupiedTablesCount(this.occupiedTablesCount);
            return dto;
        }
    }
}
