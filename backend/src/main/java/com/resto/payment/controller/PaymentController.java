package com.resto.payment.controller;

import com.resto.core.response.Response;
import com.resto.payment.domain.Payment;
import com.resto.payment.service.PaymentService;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER')")
    public Response<Payment> recordPayment(@RequestBody RecordPaymentRequest request) {
        Payment payment = paymentService.recordPayment(
                request.getOrganizationId(),
                request.getStoreId(),
                request.getOrderId(),
                request.getCashierUserId(),
                request.getPaymentMethod(),
                request.getAmount()
        );
        return Response.<Payment>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(payment)
                .build();
    }

    public static class RecordPaymentRequest {
        private UUID organizationId;
        private UUID storeId;
        private UUID orderId;
        private UUID cashierUserId;
        private String paymentMethod;
        private BigDecimal amount;

        public UUID getOrganizationId() { return organizationId; }
        public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
        public UUID getStoreId() { return storeId; }
        public void setStoreId(UUID storeId) { this.storeId = storeId; }
        public UUID getOrderId() { return orderId; }
        public void setOrderId(UUID orderId) { this.orderId = orderId; }
        public UUID getCashierUserId() { return cashierUserId; }
        public void setCashierUserId(UUID cashierUserId) { this.cashierUserId = cashierUserId; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }
}
