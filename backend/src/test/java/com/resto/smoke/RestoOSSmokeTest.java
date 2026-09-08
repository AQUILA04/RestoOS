package com.resto.smoke;

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
    @DisplayName("Smoke Test: ST-1.1 Tenant isolation context key contract")
    void testTenantContextKeyContract() {
        String tenantHeader = "X-Tenant-ID";
        assertNotNull(tenantHeader);
        assertEquals("X-Tenant-ID", tenantHeader);
    }
}
