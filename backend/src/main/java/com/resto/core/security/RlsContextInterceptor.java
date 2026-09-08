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

            if (orgIdStr != null && !orgIdStr.isBlank()) {
                try {
                    UUID orgId = UUID.fromString(orgIdStr);
                    TenantContext.setOrgId(orgId);
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (storeIdStr != null && !storeIdStr.isBlank()) {
                try {
                    UUID storeId = UUID.fromString(storeIdStr);
                    TenantContext.setStoreId(storeId);
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
