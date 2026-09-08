package com.resto.smoke;

import com.resto.order.domain.OrderStateMachine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RestoOSSmokeTest {

    @Test
    @DisplayName("Smoke Test: ST-1.0 RestoOS core backend environment readiness check")
    void testCoreBackendReadinessSmoke() {
        String version = System.getProperty("java.version");
        assertNotNull(version, "Java runtime environment must be available");
        boolean isJava21OrHigher = version.startsWith("21") || Integer.parseInt(version.split("\\.")[0]) >= 21;
        assertTrue(isJava21OrHigher, "RestoOS backend must run on Java 21 or higher");
    }

    @Test
    @DisplayName("Smoke Test: ST-4.x order state machine contract loaded")
    void testOrderStateMachineContract() {
        assertTrue(OrderStateMachine.canTransition("CREATED", "SENT_TO_KITCHEN"));
        assertTrue(OrderStateMachine.canTransition("READY", "DELIVERED"));
        assertFalse(OrderStateMachine.canTransition("READY", "PREPARING"));
        assertFalse(OrderStateMachine.canCancel("CLOSED"));
    }
}
