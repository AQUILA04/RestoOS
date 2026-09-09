package com.resto.core.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Component
public class NotificationHubClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationHubClient.class);

    private final RestClient restClient = RestClient.create();
    private final boolean enabled;
    private final String baseUrl;
    private final String tokenUri;
    private final String clientId;
    private final String clientSecret;
    private final String from;

    public NotificationHubClient(
            @Value("${restoos.notification.hub.enabled:false}") boolean enabled,
            @Value("${restoos.notification.hub.base-url:}") String baseUrl,
            @Value("${restoos.notification.hub.oauth2-token-uri:}") String tokenUri,
            @Value("${restoos.notification.hub.oauth2-client-id:restoos}") String clientId,
            @Value("${restoos.notification.hub.oauth2-client-secret:}") String clientSecret,
            @Value("${restoos.notification.hub.from:noreply@optimizesolux.com}") String from) {
        this.enabled = enabled && baseUrl != null && !baseUrl.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
        this.baseUrl = trimSlash(baseUrl);
        this.tokenUri = tokenUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.from = from;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void sendEmail(String to, String subject, String htmlBody, String idempotencyKey) {
        if (!enabled) {
            throw new IllegalStateException("Notification hub is disabled");
        }
        String token = fetchToken();
        Map<String, Object> body = Map.of(
                "channel", "EMAIL",
                "to", to,
                "subject", subject,
                "bodyHtml", htmlBody,
                "from", from
        );
        restClient.post()
                .uri(baseUrl + "/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString())
                .body(body)
                .retrieve()
                .toBodilessEntity();
        log.info("Invitation email queued via notification-hub for {}", to);
    }

    @SuppressWarnings("unchecked")
    private String fetchToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        Map<String, Object> token = restClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
        if (token == null || token.get("access_token") == null) {
            throw new IllegalStateException("Unable to obtain notification-hub token");
        }
        return token.get("access_token").toString();
    }

    private static String trimSlash(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }
}
