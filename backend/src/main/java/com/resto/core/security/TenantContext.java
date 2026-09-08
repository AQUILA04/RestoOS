package com.resto.core.security;

import java.util.UUID;

/**
 * Request-scoped tenant / actor context derived exclusively from JWT claims
 * (and membership checks). Client headers such as X-Tenant-ID are ignored for authz.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> currentOrgId = new ThreadLocal<>();
    private static final ThreadLocal<UUID> currentStoreId = new ThreadLocal<>();
    private static final ThreadLocal<UUID> currentUserId = new ThreadLocal<>();

    private TenantContext() {}

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

    public static void setUserId(UUID userId) {
        currentUserId.set(userId);
    }

    public static UUID getUserId() {
        return currentUserId.get();
    }

    public static UUID requireOrgId() {
        UUID orgId = getOrgId();
        if (orgId == null) {
            throw new IllegalStateException("organization_id missing from authenticated context");
        }
        return orgId;
    }

    public static UUID requireStoreId() {
        UUID storeId = getStoreId();
        if (storeId == null) {
            throw new IllegalStateException("store_id missing from authenticated context");
        }
        return storeId;
    }

    public static UUID requireUserId() {
        UUID userId = getUserId();
        if (userId == null) {
            throw new IllegalStateException("user id (sub) missing from authenticated context");
        }
        return userId;
    }

    public static void clear() {
        currentOrgId.remove();
        currentStoreId.remove();
        currentUserId.remove();
    }
}
