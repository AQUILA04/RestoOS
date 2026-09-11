package com.resto.payment.controller;

import com.resto.core.response.Response;
import com.resto.core.security.JwtAuth;
import com.resto.core.security.TenantContext;
import com.resto.payment.dto.CashSessionDto;
import com.resto.payment.dto.CashSessionReportDto;
import com.resto.payment.service.CashSessionPdfService;
import com.resto.payment.service.CashSessionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
public class CashSessionController {

    private final CashSessionService cashSessionService;
    private final CashSessionPdfService cashSessionPdfService;

    public CashSessionController(CashSessionService cashSessionService,
                                 CashSessionPdfService cashSessionPdfService) {
        this.cashSessionService = cashSessionService;
        this.cashSessionPdfService = cashSessionPdfService;
    }

    @PostMapping("/api/v1/stores/{storeId}/cash-sessions/open")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER')")
    public ResponseEntity<Response<CashSessionDto>> open(
            @PathVariable UUID storeId,
            @RequestBody(required = false) OpenCashSessionRequest request) {

        UUID organizationId = TenantContext.getOrgId() != null ? TenantContext.getOrgId() : JwtAuth.organizationId();
        UUID userId = TenantContext.getUserId() != null ? TenantContext.getUserId() : JwtAuth.userId();
        BigDecimal openingFloat = request != null ? request.getOpeningFloat() : null;

        CashSessionDto session = cashSessionService.open(organizationId, storeId, userId, openingFloat);
        return ResponseEntity.status(HttpStatus.CREATED).body(ok(session));
    }

    @GetMapping("/api/v1/stores/{storeId}/cash-sessions/current")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER')")
    public ResponseEntity<Response<CashSessionDto>> current(@PathVariable UUID storeId) {
        UUID userId = TenantContext.getUserId() != null ? TenantContext.getUserId() : JwtAuth.userId();
        return cashSessionService.getCurrent(storeId, userId)
                .map(dto -> ResponseEntity.ok(ok(dto)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Response.<CashSessionDto>builder()
                                .status(HttpStatus.NOT_FOUND)
                                .statusCode(HttpStatus.NOT_FOUND.value())
                                .message("error.not.found")
                                .service("RESTO-OS")
                                .data(null)
                                .build()));
    }

    @GetMapping("/api/v1/stores/{storeId}/cash-sessions")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER')")
    public Response<List<CashSessionDto>> list(
            @PathVariable UUID storeId,
            @RequestParam(required = false) UUID cashierUserId) {
        UUID actor = TenantContext.getUserId() != null ? TenantContext.getUserId() : JwtAuth.userId();
        UUID filterCashier = cashierUserId;
        if (!JwtAuth.hasAnyRole("OWNER", "ADMIN", "STORE_MANAGER")) {
            filterCashier = actor;
        }
        return ok(cashSessionService.list(storeId, filterCashier));
    }

    @GetMapping("/api/v1/cash-sessions/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER')")
    public Response<CashSessionDto> get(@PathVariable("id") UUID id) {
        return ok(cashSessionService.getById(id));
    }

    @PostMapping("/api/v1/cash-sessions/{id}/close")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER')")
    public Response<CashSessionDto> close(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) CloseCashSessionRequest request) {
        UUID actor = TenantContext.getUserId() != null ? TenantContext.getUserId() : JwtAuth.userId();
        String notes = request != null ? request.getClosingNotes() : null;
        return ok(cashSessionService.close(id, actor, notes));
    }

    @GetMapping("/api/v1/cash-sessions/{id}/report")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER')")
    public Response<CashSessionReportDto> report(@PathVariable("id") UUID id) {
        return ok(cashSessionService.getReport(id));
    }

    @GetMapping("/api/v1/cash-sessions/{id}/report.pdf")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER')")
    public ResponseEntity<byte[]> reportPdf(@PathVariable("id") UUID id) {
        CashSessionReportDto report = cashSessionService.getReport(id);
        byte[] pdf = cashSessionPdfService.generate(report);
        String filename = "rapport-caisse-" + id + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private static <T> Response<T> ok(T data) {
        return Response.<T>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(data)
                .build();
    }

    public static class OpenCashSessionRequest {
        private BigDecimal openingFloat;

        public BigDecimal getOpeningFloat() { return openingFloat; }
        public void setOpeningFloat(BigDecimal openingFloat) { this.openingFloat = openingFloat; }
    }

    public static class CloseCashSessionRequest {
        private String closingNotes;

        public String getClosingNotes() { return closingNotes; }
        public void setClosingNotes(String closingNotes) { this.closingNotes = closingNotes; }
    }
}
