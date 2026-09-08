package com.resto.core.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock IdempotencyKeyRepository repository;
    IdempotencyService service;
    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new IdempotencyService(repository, mapper);
    }

    @Test
    @DisplayName("Requires Idempotency-Key")
    void requiresKey() {
        assertThrows(IllegalArgumentException.class, () ->
                service.execute(UUID.randomUUID(), null, "POST /x", " ", MapReq.of("a"), () -> "ok", String.class));
    }

    @Test
    @DisplayName("Replays stored response without re-executing action")
    void replays() throws Exception {
        UUID org = UUID.randomUUID();
        IdempotencyKey existing = IdempotencyKey.builder()
                .organizationId(org)
                .endpoint("POST /x")
                .key("k1")
                .requestHash(service.hashRequest(MapReq.of("a")))
                .responseBody(mapper.writeValueAsString("cached"))
                .statusCode(200)
                .build();
        when(repository.findByOrganizationIdAndEndpointAndKey(org, "POST /x", "k1"))
                .thenReturn(Optional.of(existing));

        AtomicInteger calls = new AtomicInteger();
        String result = service.execute(org, null, "POST /x", "k1", MapReq.of("a"),
                () -> { calls.incrementAndGet(); return "new"; }, String.class);

        assertEquals("cached", result);
        assertEquals(0, calls.get());
    }

    @Test
    @DisplayName("Rejects key reuse with different body")
    void rejectsDifferentBody() {
        UUID org = UUID.randomUUID();
        IdempotencyKey existing = IdempotencyKey.builder()
                .organizationId(org)
                .endpoint("POST /x")
                .key("k1")
                .requestHash("different")
                .responseBody("\"x\"")
                .build();
        when(repository.findByOrganizationIdAndEndpointAndKey(org, "POST /x", "k1"))
                .thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () ->
                service.execute(org, null, "POST /x", "k1", MapReq.of("b"), () -> "ok", String.class));
    }

    @Test
    @DisplayName("Valid key format helper")
    void keyFormat() {
        assertTrue(service.isValidKeyFormat("abc-123"));
        assertFalse(service.isValidKeyFormat(""));
        assertFalse(service.isValidKeyFormat(null));
    }

    static class MapReq {
        public String v;
        static MapReq of(String v) {
            MapReq r = new MapReq();
            r.v = v;
            return r;
        }
    }
}
