package com.resto.dashboard.controller;

import com.resto.core.response.Response;
import com.resto.dashboard.dto.DashboardMetricsDto;
import com.resto.dashboard.service.DashboardService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER')")
    public Response<DashboardMetricsDto> getStoreMetrics(@RequestParam("storeId") UUID storeId) {
        DashboardMetricsDto metrics = dashboardService.getStoreMetrics(storeId);
        return Response.<DashboardMetricsDto>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(metrics)
                .build();
    }
}
