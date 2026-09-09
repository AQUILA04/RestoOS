package com.resto.tenant.service;

import com.resto.tenant.domain.Membership;
import com.resto.tenant.domain.Store;
import com.resto.tenant.domain.User;
import com.resto.tenant.repository.MembershipRepository;
import com.resto.tenant.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OidcCallbackService {

    private final KeycloakAdminClient keycloakAdminClient;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final TenantService tenantService;
    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OidcCallbackService(KeycloakAdminClient keycloakAdminClient,
                               UserRepository userRepository,
                               MembershipRepository membershipRepository,
                               TenantService tenantService,
                               AuthService authService) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.tenantService = tenantService;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> completeLogin(String code, String codeVerifier, String redirectUri) {
        if (!keycloakAdminClient.isEnabled()) {
            throw new IllegalStateException("Keycloak is not configured");
        }
        Map<String, Object> tokens = keycloakAdminClient.exchangeAuthorizationCode(code, codeVerifier, redirectUri);
        if (tokens == null || tokens.get("access_token") == null) {
            throw new IllegalStateException("OIDC token exchange failed");
        }
        String accessToken = tokens.get("access_token").toString();
        JsonNode payload = decodeJwtPayload(accessToken);
        String email = text(payload, "email");
        String keycloakSub = text(payload, "sub");
        String organizationId = text(payload, "organization_id");
        String storeId = text(payload, "store_id");

        User user = null;
        if (keycloakSub != null) {
            user = userRepository.findByKeycloakId(keycloakSub).orElse(null);
        }
        if (user == null && email != null) {
            user = userRepository.findByEmail(email).orElse(null);
        }
        if (user == null) {
            throw new IllegalArgumentException("No RestoOS account for this login");
        }

        List<Membership> memberships = membershipRepository.findByUserId(user.getId());
        if (memberships.isEmpty()) {
            throw new IllegalStateException("User has no organization membership");
        }
        Membership membership = memberships.get(0);
        UUID orgId = membership.getOrganizationId();
        if (organizationId != null && !organizationId.isBlank()) {
            try {
                orgId = UUID.fromString(organizationId);
            } catch (IllegalArgumentException ignored) {
            }
        }

        UUID resolvedStoreId = null;
        if (storeId != null && !storeId.isBlank()) {
            try {
                resolvedStoreId = UUID.fromString(storeId);
            } catch (IllegalArgumentException ignored) {
            }
        }
        List<Store> stores;
        try {
            authService.bindRls(orgId, resolvedStoreId);
            stores = tenantService.getStoresByOrganization(orgId);
        } finally {
            // TenantContext cleared by interceptor after request; keep local use only
        }
        if (resolvedStoreId == null && !stores.isEmpty()) {
            resolvedStoreId = stores.get(0).getId();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accessToken", accessToken);
        result.put("tokenType", "Bearer");
        result.put("userId", user.getId().toString());
        result.put("organizationId", orgId.toString());
        result.put("storeId", resolvedStoreId != null ? resolvedStoreId.toString() : "");
        result.put("roles", List.of(membership.getRole()));
        result.put("storeCount", stores.size());
        result.put("needsLogin", false);
        return result;
    }

    private JsonNode decodeJwtPayload(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid JWT");
            }
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to parse access token", e);
        }
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }
}
