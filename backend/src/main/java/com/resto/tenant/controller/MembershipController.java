package com.resto.tenant.controller;

import com.resto.core.response.Response;
import com.resto.core.security.JwtAuth;
import com.resto.core.security.TenantContext;
import com.resto.tenant.domain.Membership;
import com.resto.tenant.dto.OrgMemberDto;
import com.resto.tenant.service.InvitationService;
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
    private final InvitationService invitationService;

    public MembershipController(UserService userService, InvitationService invitationService) {
        this.userService = userService;
        this.invitationService = invitationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<List<OrgMemberDto>> listMembers(
            @RequestParam(value = "organizationId", required = false) UUID organizationId) {
        return ok(userService.listOrgMembers(resolveOrg(organizationId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<Membership> createMembership(@RequestBody CreateMembershipRequest request) {
        UUID organizationId = resolveOrg(request.getOrganizationId());
        Membership membership = userService.createMembership(
                organizationId,
                request.getUserId(),
                request.getRole(),
                request.getStoreIds()
        );
        return ok(membership);
    }

    @PostMapping("/invite")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<Membership> inviteUser(@RequestBody InviteRequest request) {
        UUID organizationId = resolveOrg(request.getOrganizationId());
        List<UUID> storeIds = request.getStoreId() != null
                ? List.of(request.getStoreId())
                : request.getStoreIds();
        Membership membership = invitationService.invite(
                organizationId, request.getEmail(), request.getRole(), storeIds
        );
        return ok(membership);
    }

    @PatchMapping("/{membershipId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<OrgMemberDto> updateMembership(
            @PathVariable("membershipId") UUID membershipId,
            @RequestBody UpdateMembershipRequest request) {
        if (request.getRole() != null) {
            userService.updateMembershipRole(membershipId, request.getRole());
        }
        if (request.getStoreIds() != null) {
            userService.replaceMembershipStores(membershipId, request.getStoreIds());
        }
        if (request.getActive() != null && request.getUserId() != null) {
            userService.setUserActive(request.getUserId(), request.getActive());
        }
        UUID orgId = resolveOrg(null);
        return ok(userService.listOrgMembers(orgId).stream()
                .filter(m -> membershipId.equals(m.getMembershipId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + membershipId)));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER')")
    public Response<List<Membership>> getMembershipsByUser(@PathVariable("userId") UUID userId) {
        return ok(userService.getMembershipsByUser(userId));
    }

    private UUID resolveOrg(UUID requestOrg) {
        if (TenantContext.getOrgId() != null) return TenantContext.getOrgId();
        try {
            return JwtAuth.organizationId();
        } catch (Exception e) {
            if (requestOrg != null) return requestOrg;
            throw new IllegalStateException("organization_id required");
        }
    }

    private <T> Response<T> ok(T data) {
        return Response.<T>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(data)
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
        private UUID storeId;
        private List<UUID> storeIds;
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

    public static class UpdateMembershipRequest {
        private UUID userId;
        private String role;
        private Boolean active;
        private List<UUID> storeIds;
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
        public List<UUID> getStoreIds() { return storeIds; }
        public void setStoreIds(List<UUID> storeIds) { this.storeIds = storeIds; }
    }
}
