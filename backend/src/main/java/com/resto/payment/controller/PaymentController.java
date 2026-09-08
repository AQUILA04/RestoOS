package com.resto.payment.controller;

import com.resto.core.idempotency.IdempotencyService;
import com.resto.core.response.Response;
import com.resto.core.security.JwtAuth;
import com.resto.core.security.TenantContext;
import com.resto.payment.domain.Payment;
import com.resto.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
public class PaymentController {

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    public PaymentController(PaymentService paymentService, IdempotencyService idempotencyService) {
        this.paymentService = paymentService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/api/v1/orders/{id}/payment/mark-paid")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER')")
    public Response<Payment> markPaid(
            @PathVariable("id") UUID orderId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody MarkPaidRequest request) {

        UUID organizationId = TenantContext.getOrgId() != null ? TenantContext.getOrgId() : JwtAuth.organizationId();
        UUID storeId = TenantContext.getStoreId();
        UUID cashier = TenantContext.getUserId() != null ? TenantContext.getUserId() : JwtAuth.userId();

        Payment payment = idempotencyService.execute(
                organizationId,
                storeId,
                "POST /api/v1/orders/" + orderId + "/payment/mark-paid",
                idempotencyKey,
                request,
                () -> paymentService.markPaid(
                        organizationId,
                        storeId,
                        orderId,
                        cashier,
                        request.getPaymentMethod() != null ? request.getPaymentMethod() : request.getMethod(),
                        request.getAmount()
                ),
                Payment.class
        );

        return Response.<Payment>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(payment)
                .build();
    }

    /** Legacy path kept for compatibility */
    @PostMapping("/api/v1/payments")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER')")
    public Response<Payment> recordPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody MarkPaidRequest request) {
        UUID organizationId = TenantContext.getOrgId() != null ? TenantContext.getOrgId() : JwtAuth.organizationId();
        UUID storeId = request.getStoreId() != null ? request.getStoreId() : TenantContext.getStoreId();
        UUID cashier = TenantContext.getUserId() != null ? TenantContext.getUserId() : JwtAuth.userId();
        String key = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();

        Payment payment = idempotencyService.execute(
                organizationId, storeId, "POST /api/v1/payments", key, request,
                () -> paymentService.markPaid(organizationId, storeId, request.getOrderId(), cashier,
                        request.getPaymentMethod() != null ? request.getPaymentMethod() : request.getMethod(),
                        request.getAmount()),
                Payment.class
        );

        return Response.<Payment>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(payment)
                .build();
    }

    public static class MarkPaidRequest {
        private UUID orderId;
        private UUID storeId;
        private String method;
        private String paymentMethod;
        private BigDecimal amount;

        public UUID getOrderId() { return orderId; }
        public void setOrderId(UUID orderId) { this.orderId = orderId; }
        public UUID getStoreId() { return storeId; }
        public void setStoreId(UUID storeId) { this.storeId = storeId; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }
}
