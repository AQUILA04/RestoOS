package com.resto.core.security;

import java.util.UUID;

public class TenantContext {
    private static final ThreadLocal<UUID> currentOrgId = new ThreadLocal<>();
    private static final ThreadLocal<UUID> currentStoreId = new ThreadLocal<>();

    public static void setOrgId(UUID orgId) {
        currentOrgId.set(orgId);
    }

    public static UUID getOrgId() {
        return currentOrgId.get();
    }

    public static void setStoreId(UUID storeId) {
        currentStoreId.set(storeId);
    }

    public static UUID getStoreId() {
        return currentStoreId.get();
    }

    public static void clear() {
        currentOrgId.remove();
        currentStoreId.remove();
    }
}
