package com.resto.core.security;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Populates {@link TenantContext} from JWT claims only.
 * Deprecated {@code X-Tenant-ID} header is ignored for authorization.
 */
@Component
public class RlsContextInterceptor implements HandlerInterceptor {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String orgIdStr = jwt.getClaimAsString("organization_id");
            String storeIdStr = jwt.getClaimAsString("store_id");
            String userIdStr = jwt.getClaimAsString("user_id");
            if (userIdStr == null || userIdStr.isBlank()) {
                userIdStr = jwt.getSubject();
            }

            if (orgIdStr != null && !orgIdStr.isBlank()) {
                try {
                    TenantContext.setOrgId(UUID.fromString(orgIdStr));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (storeIdStr != null && !storeIdStr.isBlank()) {
                try {
                    TenantContext.setStoreId(UUID.fromString(storeIdStr));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (userIdStr != null && !userIdStr.isBlank()) {
                try {
                    TenantContext.setUserId(UUID.fromString(userIdStr));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        TenantContext.clear();
    }
}
