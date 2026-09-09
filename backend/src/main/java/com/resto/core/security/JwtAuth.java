package com.resto.core.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Helpers to read JWT claims. organizationId / storeId / userId MUST come from JWT only.
 */
public final class JwtAuth {

    private JwtAuth() {}

    public static Optional<Jwt> currentJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token) {
            return Optional.of(token.getToken());
        }
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt);
        }
        return Optional.empty();
    }

    public static UUID userId() {
        return currentJwt()
                .map(jwt -> {
                    String sub = jwt.getSubject();
                    // Prefer explicit user_id claim (station PIN tokens / Keycloak mapped claims)
                    String userIdClaim = jwt.getClaimAsString("user_id");
                    String raw = userIdClaim != null && !userIdClaim.isBlank() ? userIdClaim : sub;
                    return UUID.fromString(raw);
                })
                .orElseThrow(() -> new IllegalStateException("Unauthenticated: no JWT subject"));
    }

    public static UUID organizationId() {
        return currentJwt()
                .map(jwt -> jwt.getClaimAsString("organization_id"))
                .filter(s -> s != null && !s.isBlank())
                .map(UUID::fromString)
                .orElseGet(TenantContext::requireOrgId);
    }

    public static Optional<UUID> storeId() {
        return currentJwt()
                .map(jwt -> jwt.getClaimAsString("store_id"))
                .filter(s -> s != null && !s.isBlank())
                .map(UUID::fromString)
                .or(() -> Optional.ofNullable(TenantContext.getStoreId()));
    }

    public static List<String> roles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return List.of();
        }
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toList());
    }

    public static boolean hasAnyRole(String... roles) {
        List<String> current = roles();
        for (String role : roles) {
            if (current.contains(role)) {
                return true;
            }
        }
        return false;
    }
}
