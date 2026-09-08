package com.resto.unit;

import com.resto.core.response.Response;
import com.resto.order.domain.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderUnitTest {

    @Test
    @DisplayName("Unit Test: Order domain builder defaults and calculations")
    void testOrderBuilderAndDefaults() {
        UUID orgId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        Order order = Order.builder()
                .organizationId(orgId)
                .storeId(storeId)
                .orderNumber(1001)
                .subtotal(new BigDecimal("15.00"))
                .taxTotal(new BigDecimal("3.00"))
                .totalAmount(new BigDecimal("18.00"))
                .build();

        assertNotNull(order);
        assertEquals(orgId, order.getOrganizationId());
        assertEquals(storeId, order.getStoreId());
        assertEquals(1001, order.getOrderNumber());
        assertEquals("DINE_IN", order.getOrderType());
        assertEquals("CREATED", order.getStatus());
        assertEquals("UNPAID", order.getPaymentStatus());
        assertEquals(new BigDecimal("18.00"), order.getTotalAmount());
    }

    @Test
    @DisplayName("Unit Test: Response API envelope builder formatting")
    void testResponseEnvelopeBuilder() {
        Response<String> response = Response.<String>builder()
                .status(HttpStatus.OK)
                .statusCode(200)
                .message("default.message.success")
                .data("RestoOS Service Online")
                .build();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(200, response.getStatusCode());
        assertEquals("RESTO-OS", response.getService());
        assertEquals("RestoOS Service Online", response.getData());
    }
}
