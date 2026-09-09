package com.resto.tenant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Optional Keycloak Admin + token helper. Disabled when base URL or secret is blank.
 */
@Component
public class KeycloakAdminClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClient.class);

    private final RestClient restClient;
    private final String baseUrl;
    private final String realm;
    private final String adminClientId;
    private final String adminClientSecret;
    private final String publicClientId;
    private final boolean enabled;

    public KeycloakAdminClient(
            @Value("${restoos.keycloak.base-url:}") String baseUrl,
            @Value("${restoos.keycloak.realm:restoos}") String realm,
            @Value("${restoos.keycloak.admin-client-id:restoos-backend}") String adminClientId,
            @Value("${restoos.keycloak.admin-client-secret:}") String adminClientSecret,
            @Value("${restoos.keycloak.public-client-id:restoos-frontend}") String publicClientId) {
        this.baseUrl = trimSlash(baseUrl);
        this.realm = realm;
        this.adminClientId = adminClientId;
        this.adminClientSecret = adminClientSecret;
        this.publicClientId = publicClientId;
        this.enabled = this.baseUrl != null && !this.baseUrl.isBlank()
                && adminClientSecret != null && !adminClientSecret.isBlank();
        this.restClient = RestClient.create();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String createUser(String email, String firstName, String lastName, String password) {
        String token = clientCredentialsToken();
        Map<String, Object> body = new HashMap<>();
        body.put("username", email);
        body.put("email", email);
        body.put("enabled", true);
        body.put("emailVerified", true);
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        body.put("credentials", List.of(Map.of(
                "type", "password",
                "value", password,
                "temporary", false
        )));

        var response = restClient.post()
                .uri(baseUrl + "/admin/realms/" + realm + "/users")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        String location = response.getHeaders().getFirst("Location");
        if (location == null || location.isBlank()) {
            throw new IllegalStateException("Keycloak did not return user Location");
        }
        return location.substring(location.lastIndexOf('/') + 1);
    }

    public void setUserAttributes(String userId, String organizationId, String storeId) {
        String token = clientCredentialsToken();
        Map<String, Object> user = restClient.get()
                .uri(baseUrl + "/admin/realms/" + realm + "/users/" + userId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(Map.class);
        if (user == null) {
            return;
        }
        Map<String, Object> attributes = new HashMap<>();
        Object existing = user.get("attributes");
        if (existing instanceof Map<?, ?> map) {
            map.forEach((k, v) -> attributes.put(String.valueOf(k), v));
        }
        attributes.put("organization_id", List.of(organizationId));
        attributes.put("store_id", List.of(storeId));
        user.put("attributes", attributes);
        restClient.put()
                .uri(baseUrl + "/admin/realms/" + realm + "/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .body(user)
                .retrieve()
                .toBodilessEntity();
    }

    public void assignRealmRole(String userId, String roleName) {
        try {
            String token = clientCredentialsToken();
            Map role = restClient.get()
                    .uri(baseUrl + "/admin/realms/" + realm + "/roles/" + roleName)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(Map.class);
            if (role == null) {
                return;
            }
            restClient.post()
                    .uri(baseUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .body(List.of(role))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Could not assign realm role {}: {}", roleName, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> passwordGrant(String username, String password) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "password");
            form.add("client_id", publicClientId);
            form.add("username", username);
            form.add("password", password);
            return restClient.post()
                    .uri(baseUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            log.warn("Password grant failed: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> exchangeAuthorizationCode(String code, String codeVerifier, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", publicClientId);
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        if (codeVerifier != null && !codeVerifier.isBlank()) {
            form.add("code_verifier", codeVerifier);
        }
        return restClient.post()
                .uri(baseUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    private String clientCredentialsToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", adminClientId);
        form.add("client_secret", adminClientSecret);
        Map<String, Object> token = restClient.post()
                .uri(baseUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
        if (token == null || token.get("access_token") == null) {
            throw new IllegalStateException("Unable to obtain Keycloak admin token");
        }
        return token.get("access_token").toString();
    }

    private static String trimSlash(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }
}
