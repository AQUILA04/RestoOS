package com.resto.tenant.service;

import com.resto.tenant.domain.Membership;
import com.resto.tenant.domain.MembershipStore;
import com.resto.tenant.domain.User;
import com.resto.tenant.repository.MembershipRepository;
import com.resto.tenant.repository.MembershipStoreRepository;
import com.resto.tenant.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipStoreRepository membershipStoreRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecretKey jwtSecret;
    private final long stationTtlMinutes;

    /** Simple in-memory rate limit: userId → attempts in window */
    private final ConcurrentHashMap<UUID, AttemptWindow> pinAttempts = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository,
                       MembershipRepository membershipRepository,
                       MembershipStoreRepository membershipStoreRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${restoos.jwt.secret}") String secret,
                       @Value("${restoos.jwt.station-ttl-minutes:480}") long stationTtlMinutes) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.membershipStoreRepository = membershipStoreRepository;
        this.passwordEncoder = passwordEncoder;
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
            keyBytes = padded;
        }
        this.jwtSecret = Keys.hmacShaKeyFor(keyBytes);
        this.stationTtlMinutes = stationTtlMinutes;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> pinLogin(UUID userId, UUID storeId, UUID organizationId, String rawPin) {
        if (rawPin == null || !rawPin.matches("\\d{4}")) {
            throw new IllegalArgumentException("PIN must be exactly 4 digits");
        }
        checkRateLimit(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getPinHash() == null || !passwordEncoder.matches(rawPin, user.getPinHash())) {
            recordFailure(userId);
            return Map.of("authenticated", false);
        }

        Membership membership = membershipRepository.findByUserId(userId).stream()
                .filter(m -> organizationId == null || organizationId.equals(m.getOrganizationId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No membership for user"));

        UUID orgId = membership.getOrganizationId();
        UUID resolvedStoreId = storeId;
        String role = membership.getRole();

        if (resolvedStoreId != null && !"OWNER".equals(role) && !"ADMIN".equals(role)) {
            List<MembershipStore> stores = membershipStoreRepository.findByMembershipId(membership.getId());
            boolean ok = stores.isEmpty() || stores.stream().anyMatch(ms -> resolvedStoreId.equals(ms.getStoreId()));
            if (!ok) {
                throw new IllegalArgumentException("User is not a member of store: " + resolvedStoreId);
            }
        }

        pinAttempts.remove(userId);

        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("user_id", userId.toString())
                .claim("organization_id", orgId.toString())
                .claim("store_id", resolvedStoreId != null ? resolvedStoreId.toString() : null)
                .claim("roles", List.of(role))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(stationTtlMinutes, ChronoUnit.MINUTES)))
                .signWith(jwtSecret)
                .compact();

        return Map.of(
                "authenticated", true,
                "accessToken", token,
                "tokenType", "Bearer",
                "userId", userId.toString(),
                "organizationId", orgId.toString(),
                "storeId", resolvedStoreId != null ? resolvedStoreId.toString() : "",
                "roles", List.of(role)
        );
    }

    @Transactional
    public void setPin(UUID userId, String rawPin) {
        if (rawPin == null || !rawPin.matches("\\d{4}")) {
            throw new IllegalArgumentException("PIN must be exactly 4 digits");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setPinHash(passwordEncoder.encode(rawPin));
        userRepository.save(user);
    }

    /** @deprecated prefer pinLogin returning station JWT */
    public boolean validatePin(UUID userId, String rawPin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        if (user.getPinHash() == null) {
            return false;
        }
        return passwordEncoder.matches(rawPin, user.getPinHash());
    }

    private void checkRateLimit(UUID userId) {
        AttemptWindow window = pinAttempts.get(userId);
        if (window != null && window.isOpen() && window.count.get() >= 5) {
            throw new IllegalStateException("Too many PIN attempts; try again later");
        }
    }

    private void recordFailure(UUID userId) {
        pinAttempts.compute(userId, (id, existing) -> {
            if (existing == null || !existing.isOpen()) {
                return new AttemptWindow();
            }
            existing.count.incrementAndGet();
            return existing;
        });
    }

    private static final class AttemptWindow {
        final long startedAt = System.currentTimeMillis();
        final AtomicInteger count = new AtomicInteger(1);

        boolean isOpen() {
            return System.currentTimeMillis() - startedAt < 60_000;
        }
    }
}
