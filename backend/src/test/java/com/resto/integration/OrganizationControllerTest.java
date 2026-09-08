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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SpringBootTest companion (surefire-included) for organization API + JWT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationControllerTest {

    private static final String SECRET = "test-secret-key-which-is-long-enough-123456";

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("SpringBootTest H2: create organization with HS256 JWT")
    void createOrg() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        UUID userId = UUID.randomUUID();
        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("user_id", userId.toString())
                .claim("organization_id", UUID.randomUUID().toString())
                .claim("roles", List.of("OWNER"))
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();

        mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Surefire Bistro\",\"code\":\"SF-" + UUID.randomUUID().toString().substring(0, 8) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("RESTO-OS"))
                .andExpect(jsonPath("$.data.name").value("Surefire Bistro"));
    }
}
