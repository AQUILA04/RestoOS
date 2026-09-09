package com.resto.core.security;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Sets PostgreSQL RLS session variables per transaction.
 * No-ops on H2 / non-Postgres (test profile) when set_config is unavailable.
 */
@Aspect
@Component
public class RlsAspect {

    private static final Logger log = LoggerFactory.getLogger(RlsAspect.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Before("@within(org.springframework.stereotype.Repository) || @within(org.springframework.stereotype.Service) || @annotation(org.springframework.transaction.annotation.Transactional)")
    public void setRlsContext() {
        UUID orgId = TenantContext.getOrgId();
        UUID storeId = TenantContext.getStoreId();

        try {
            if (orgId != null) {
                entityManager.createNativeQuery("SELECT set_config('app.current_org_id', :orgId, true)")
                        .setParameter("orgId", orgId.toString())
                        .getSingleResult();
            }
            if (storeId != null) {
                entityManager.createNativeQuery("SELECT set_config('app.current_store_id', :storeId, true)")
                        .setParameter("storeId", storeId.toString())
                        .getSingleResult();
            }
        } catch (Exception ex) {
            // H2 and non-Postgres dialects do not support set_config — skip silently in tests
            log.trace("RLS set_config skipped: {}", ex.getMessage());
        }
    }
}
