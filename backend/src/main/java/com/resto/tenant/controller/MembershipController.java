package com.resto.tenant.controller;

import com.resto.core.response.Response;
import com.resto.tenant.domain.Membership;
import com.resto.tenant.domain.User;
import com.resto.tenant.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/memberships")
public class MembershipController {

    private final UserService userService;

    public MembershipController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<Membership> createMembership(@RequestBody CreateMembershipRequest request) {
        Membership membership = userService.createMembership(
                request.getOrganizationId(),
                request.getUserId(),
                request.getRole(),
                request.getStoreIds()
        );
        return Response.<Membership>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(membership)
                .build();
    }

    /**
     * E2E-friendly invite endpoint: POST /api/v1/memberships/invite
     * Payload: { email, role, storeId } + X-Tenant-ID header.
     * Finds or creates the user by email, then creates the membership.
     */
    @PostMapping("/invite")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<Membership> inviteUser(
            @RequestHeader(value = "X-Tenant-ID", required = false) UUID tenantId,
            @RequestBody InviteRequest request) {

        UUID organizationId = tenantId != null ? tenantId : request.getOrganizationId();

        // Find or create user by email
        User user = userService.findOrCreateUserByEmail(request.getEmail());

        List<UUID> storeIds = request.getStoreId() != null
                ? List.of(request.getStoreId())
                : request.getStoreIds();

        Membership membership = userService.createMembership(
                organizationId,
                user.getId(),
                request.getRole(),
                storeIds
        );
        return Response.<Membership>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(membership)
                .build();
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER')")
    public Response<List<Membership>> getMembershipsByUser(@PathVariable("userId") UUID userId) {
        List<Membership> memberships = userService.getMembershipsByUser(userId);
        return Response.<List<Membership>>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(memberships)
                .build();
    }

    public static class CreateMembershipRequest {
        private UUID organizationId;
        private UUID userId;
        private String role;
        private List<UUID> storeIds;

        public UUID getOrganizationId() { return organizationId; }
        public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public List<UUID> getStoreIds() { return storeIds; }
        public void setStoreIds(List<UUID> storeIds) { this.storeIds = storeIds; }
    }

    public static class InviteRequest {
        private UUID organizationId;
        private String email;
        private String role;
        private UUID storeId;        // single store (E2E pattern)
        private List<UUID> storeIds; // multi-store (internal pattern)

        public UUID getOrganizationId() { return organizationId; }
        public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public UUID getStoreId() { return storeId; }
        public void setStoreId(UUID storeId) { this.storeId = storeId; }
        public List<UUID> getStoreIds() { return storeIds; }
        public void setStoreIds(List<UUID> storeIds) { this.storeIds = storeIds; }
    }
}
