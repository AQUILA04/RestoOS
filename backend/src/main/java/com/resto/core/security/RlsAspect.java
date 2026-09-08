package com.resto.core.security;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
public class RlsAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Before("@within(org.springframework.stereotype.Repository) || @within(org.springframework.stereotype.Service) || @annotation(org.springframework.transaction.annotation.Transactional)")
    public void setRlsContext() {
        UUID orgId = TenantContext.getOrgId();
        UUID storeId = TenantContext.getStoreId();

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
    }
}
