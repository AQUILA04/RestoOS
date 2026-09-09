package com.resto.integration;

import com.resto.core.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lightweight contract checks until Testcontainers is available in CI runners without Docker.
 */
class ApiEnvelopeIT {

    @Test
    @DisplayName("Response envelope always carries RESTO-OS service")
    void envelopeContract() {
        Response<String> response = Response.<String>builder()
                .status(HttpStatus.OK)
                .statusCode(200)
                .message("default.message.success")
                .data("ok")
                .build();
        assertEquals("RESTO-OS", response.getService());
        assertEquals(200, response.getStatusCode());
        assertEquals("ok", response.getData());
    }
}
