package com.resto.core.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * DB-backed idempotency for critical POST endpoints (orders, payments).
 */
@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyKeyRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public String hashRequest(Object body) {
        try {
            String json = body == null ? "" : objectMapper.writeValueAsString(body);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash idempotency request", e);
        }
    }

    @Transactional
    public <T> T execute(UUID organizationId, UUID storeId, String endpoint, String idempotencyKey,
                         Object requestBody, Supplier<T> action, Class<T> responseType) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }

        String requestHash = hashRequest(requestBody);
        Optional<IdempotencyKey> existing = repository.findByOrganizationIdAndEndpointAndKey(
                organizationId, endpoint, idempotencyKey);

        if (existing.isPresent()) {
            return replay(existing.get(), requestHash, responseType);
        }

        T result = action.get();
        try {
            IdempotencyKey record = IdempotencyKey.builder()
                    .organizationId(organizationId)
                    .storeId(storeId)
                    .endpoint(endpoint)
                    .key(idempotencyKey)
                    .requestHash(requestHash)
                    .build();
            record.setResponseBody(objectMapper.writeValueAsString(result));
            record.setStatusCode(200);
            repository.saveAndFlush(record);
        } catch (DataIntegrityViolationException dup) {
            // Concurrent request won the insert — return the stored response
            IdempotencyKey winner = repository.findByOrganizationIdAndEndpointAndKey(
                            organizationId, endpoint, idempotencyKey)
                    .orElseThrow(() -> dup);
            return replay(winner, requestHash, responseType);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to persist idempotent response", e);
        }
        return result;
    }

    private <T> T replay(IdempotencyKey key, String requestHash, Class<T> responseType) {
        if (!key.getRequestHash().equals(requestHash)) {
            throw new IllegalArgumentException("Idempotency-Key reused with different request body");
        }
        if (key.getResponseBody() == null) {
            throw new IllegalStateException("Idempotent request is still in progress");
        }
        try {
            return objectMapper.readValue(key.getResponseBody(), responseType);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored idempotent response is corrupt", e);
        }
    }

    /** In-memory style helper for unit tests without DB. */
    public boolean isValidKeyFormat(String key) {
        return key != null && !key.isBlank() && key.length() <= 255;
    }
}
