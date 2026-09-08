package com.resto.core.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.resto.core.response.Response;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Test/E2E only helper to mint HS256 JWTs without Keycloak.
 * Disabled outside test/e2e profiles.
 */
@RestController
@RequestMapping("/api/v1/test")
@Profile({"test", "e2e"})
public class TestTokenController {

    private final SecretKey jwtSecret;

    public TestTokenController(@Value("${restoos.jwt.secret}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
            keyBytes = padded;
        }
        this.jwtSecret = Keys.hmacShaKeyFor(keyBytes);
    }

    @PostMapping("/token")
    public Response<Map<String, String>> mint(@RequestBody TokenRequest request) {
        UUID userId = request.getUserId() != null ? request.getUserId() : UUID.randomUUID();
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("user_id", userId.toString())
                .claim("roles", request.getRoles() != null ? request.getRoles() : List.of("OWNER"))
                .audience().add("restoos-backend").and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(8, ChronoUnit.HOURS)))
                .signWith(jwtSecret);
        if (request.getOrganizationId() != null) {
            builder.claim("organization_id", request.getOrganizationId().toString());
        }
        if (request.getStoreId() != null) {
            builder.claim("store_id", request.getStoreId().toString());
        }
        String token = builder.compact();
        return Response.<Map<String, String>>builder()
                .status(HttpStatus.OK)
                .statusCode(200)
                .message("default.message.success")
                .service("RESTO-OS")
                .data(Map.of("accessToken", token, "userId", userId.toString()))
                .build();
    }

    public static class TokenRequest {
        private UUID userId;
        private UUID organizationId;
        private UUID storeId;
        private List<String> roles;
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        public UUID getOrganizationId() { return organizationId; }
        public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
        public UUID getStoreId() { return storeId; }
        public void setStoreId(UUID storeId) { this.storeId = storeId; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
    }
}
