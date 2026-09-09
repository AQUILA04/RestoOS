package com.resto.integration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Golden-path API contract smoke on H2 + HS256 test JWT.
 * Included in surefire via pom so {@code mvn test} exercises it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiContractSmokeIT extends AbstractH2SpringBootTest {

    private static final String SECRET = "test-secret-key-which-is-long-enough-123456";

    @Autowired
    MockMvc mockMvc;

    private String bearer(UUID userId, UUID orgId, String... roles) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return "Bearer " + Jwts.builder()
                .subject(userId.toString())
                .claim("user_id", userId.toString())
                .claim("organization_id", orgId.toString())
                .claim("roles", List.of(roles))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("API envelope + create organization under test JWT")
    void createOrganizationContract() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orgClaim = UUID.randomUUID();
        String code = "SMOKE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", bearer(userId, orgClaim, "OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Smoke Bistro\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.service").value("RESTO-OS"))
                .andExpect(jsonPath("$.data.name").value("Smoke Bistro"))
                .andExpect(jsonPath("$.data.code").value(code))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    @DisplayName("Actuator info is public; organizations require auth")
    void authBoundary() throws Exception {
        // Prefer /info over /health: health may be 503 when optional mail/redis probes are down.
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/organizations"))
                .andExpect(status().isUnauthorized());
    }
}
