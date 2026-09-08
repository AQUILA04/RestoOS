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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationControllerIT extends AbstractH2SpringBootTest {

    private static final String SECRET = "test-secret-key-which-is-long-enough-123456";

    @Autowired
    MockMvc mockMvc;

    private String bearer(UUID userId, UUID orgId, String... roles) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("user_id", userId.toString())
                .claim("organization_id", orgId.toString())
                .claim("roles", List.of(roles))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
        return "Bearer " + token;
    }

    @Test
    @DisplayName("POST /api/v1/organizations creates org under JWT auth")
    void createOrganization() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orgClaim = UUID.randomUUID();
        String token = bearer(userId, orgClaim, "OWNER");

        mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bistro Test","code":"BISTRO-IT"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.service").value("RESTO-OS"))
                .andExpect(jsonPath("$.data.name").value("Bistro Test"))
                .andExpect(jsonPath("$.data.code").value("BISTRO-IT"));
    }

    @Test
    @DisplayName("Unauthenticated requests are rejected (never permitAll)")
    void rejectsAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/organizations"))
                .andExpect(status().isUnauthorized());
    }
}
