package com.resto.core.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

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

    @Test
    @DisplayName("First call executes action and persists response for replay")
    void firstCallPersists() {
        UUID org = UUID.randomUUID();
        when(repository.findByOrganizationIdAndEndpointAndKey(org, "POST /pay", "k-new"))
                .thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(IdempotencyKey.class))).thenAnswer(inv -> inv.getArgument(0));

        AtomicInteger calls = new AtomicInteger();
        String result = service.execute(org, null, "POST /pay", "k-new", MapReq.of("a"),
                () -> { calls.incrementAndGet(); return "created"; }, String.class);

        assertEquals("created", result);
        assertEquals(1, calls.get());
        verify(repository).saveAndFlush(argThat(key ->
                "k-new".equals(key.getKey())
                        && key.getResponseBody() != null
                        && key.getResponseBody().contains("created")
                        && Integer.valueOf(200).equals(key.getStatusCode())));
    }

    @Test
    @DisplayName("Second call after first save replays without re-executing")
    void concurrentReplayAfterFirstSave() throws Exception {
        UUID org = UUID.randomUUID();
        String endpoint = "POST /orders";
        String key = "concurrent-1";
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger finds = new AtomicInteger();

        when(repository.findByOrganizationIdAndEndpointAndKey(org, endpoint, key)).thenAnswer(inv -> {
            int n = finds.incrementAndGet();
            if (n == 1) {
                return Optional.empty();
            }
            IdempotencyKey stored = IdempotencyKey.builder()
                    .organizationId(org)
                    .endpoint(endpoint)
                    .key(key)
                    .requestHash(service.hashRequest(MapReq.of("same")))
                    .responseBody(mapper.writeValueAsString("winner"))
                    .statusCode(200)
                    .build();
            return Optional.of(stored);
        });
        when(repository.saveAndFlush(any(IdempotencyKey.class))).thenAnswer(inv -> inv.getArgument(0));

        String first = service.execute(org, null, endpoint, key, MapReq.of("same"),
                () -> { calls.incrementAndGet(); return "winner"; }, String.class);
        String second = service.execute(org, null, endpoint, key, MapReq.of("same"),
                () -> { calls.incrementAndGet(); return "loser"; }, String.class);

        assertEquals("winner", first);
        assertEquals("winner", second);
        assertEquals(1, calls.get());
        verify(repository, times(1)).saveAndFlush(any(IdempotencyKey.class));
    }

    @Test
    @DisplayName("Unique constraint race replays winner response without failing")
    void uniqueConstraintRaceReplaysWinner() throws Exception {
        UUID org = UUID.randomUUID();
        String endpoint = "POST /orders";
        String key = "race-1";
        IdempotencyKey winner = IdempotencyKey.builder()
                .organizationId(org)
                .endpoint(endpoint)
                .key(key)
                .requestHash(service.hashRequest(MapReq.of("same")))
                .responseBody(mapper.writeValueAsString("winner"))
                .statusCode(200)
                .build();

        when(repository.findByOrganizationIdAndEndpointAndKey(org, endpoint, key))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(repository.saveAndFlush(any(IdempotencyKey.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        AtomicInteger calls = new AtomicInteger();
        String result = service.execute(org, null, endpoint, key, MapReq.of("same"),
                () -> { calls.incrementAndGet(); return "loser"; }, String.class);

        assertEquals("winner", result);
        assertEquals(1, calls.get());
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
