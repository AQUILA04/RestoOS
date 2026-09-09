package com.resto.tenant.service;

import com.resto.core.security.TenantContext;
import com.resto.tenant.domain.Membership;
import com.resto.tenant.domain.Organization;
import com.resto.tenant.domain.Store;
import com.resto.tenant.domain.User;
import com.resto.tenant.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SignupService {

    private final TenantService tenantService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final KeycloakAdminClient keycloakAdminClient;
    private final boolean hmacSessionEnabled;
    private final ConcurrentHashMap<String, AttemptWindow> signupAttempts = new ConcurrentHashMap<>();

    public SignupService(TenantService tenantService,
                         UserService userService,
                         UserRepository userRepository,
                         AuthService authService,
                         KeycloakAdminClient keycloakAdminClient,
                         @Value("${restoos.auth.hmac-session-enabled:true}") boolean hmacSessionEnabled) {
        this.tenantService = tenantService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.authService = authService;
        this.keycloakAdminClient = keycloakAdminClient;
        this.hmacSessionEnabled = hmacSessionEnabled;
    }

    @Transactional
    public Map<String, Object> signup(String organizationName,
                                      String firstName,
                                      String lastName,
                                      String email,
                                      String password,
                                      boolean multiStore) {
        if (organizationName == null || organizationName.isBlank()) {
            throw new IllegalArgumentException("organizationName is required");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("password must be at least 8 characters");
        }
        checkRateLimit(email);

        if (userRepository.existsByEmail(email.trim().toLowerCase())) {
            throw new IllegalArgumentException("User email already exists: " + email);
        }

        String orgName = organizationName.trim();
        String code = slug(orgName) + "-" + (System.currentTimeMillis() % 100000);
        Organization org = tenantService.createOrganization(orgName, code);

        TenantContext.setOrgId(org.getId());
        authService.bindRls(org.getId(), null);

        Store store = tenantService.createStore(
                org.getId(),
                orgName,
                "main",
                "Europe/Paris",
                "EUR"
        );

        String keycloakId = null;
        if (keycloakAdminClient.isEnabled()) {
            keycloakId = keycloakAdminClient.createUser(email.trim().toLowerCase(), firstName, lastName, password);
            keycloakAdminClient.setUserAttributes(keycloakId, org.getId().toString(), store.getId().toString());
            keycloakAdminClient.assignRealmRole(keycloakId, "OWNER");
        }

        User user = userService.createUser(
                email.trim().toLowerCase(),
                firstName != null ? firstName.trim() : null,
                lastName != null ? lastName.trim() : null,
                keycloakId
        );
        user.setActive(true);
        userRepository.save(user);

        Membership membership = userService.createMembership(
                org.getId(),
                user.getId(),
                "OWNER",
                List.of(store.getId())
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("organizationId", org.getId().toString());
        result.put("storeId", store.getId().toString());
        result.put("userId", user.getId().toString());
        result.put("roles", List.of(membership.getRole()));
        result.put("storeCount", 1);
        result.put("multiStorePreferred", multiStore);

        if (hmacSessionEnabled) {
            String token = authService.issueSessionToken(user.getId(), org.getId(), store.getId(), "OWNER");
            result.put("accessToken", token);
            result.put("tokenType", "Bearer");
            result.put("needsLogin", false);
        } else if (keycloakAdminClient.isEnabled()) {
            Map<String, Object> tokens = keycloakAdminClient.passwordGrant(email.trim().toLowerCase(), password);
            if (tokens != null && tokens.get("access_token") != null) {
                result.put("accessToken", tokens.get("access_token"));
                result.put("tokenType", "Bearer");
                result.put("needsLogin", false);
            } else {
                result.put("needsLogin", true);
            }
        } else {
            result.put("needsLogin", true);
        }
        return result;
    }

    private void checkRateLimit(String email) {
        String key = email.trim().toLowerCase();
        AttemptWindow window = signupAttempts.get(key);
        if (window != null && window.isOpen() && window.count.get() >= 8) {
            throw new IllegalStateException("Too many signup attempts; try again later");
        }
        signupAttempts.compute(key, (id, existing) -> {
            if (existing == null || !existing.isOpen()) {
                return new AttemptWindow();
            }
            existing.count.incrementAndGet();
            return existing;
        });
    }

    private static String slug(String value) {
        String cleaned = value.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (cleaned.isBlank()) {
            cleaned = "enseigne";
        }
        return cleaned.substring(0, Math.min(cleaned.length(), 40));
    }

    private static final class AttemptWindow {
        final long startedAt = System.currentTimeMillis();
        final AtomicInteger count = new AtomicInteger(1);

        boolean isOpen() {
            return System.currentTimeMillis() - startedAt < 60_000;
        }
    }
}
