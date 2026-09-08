package com.resto.tenant.controller;

import com.resto.core.response.Response;
import com.resto.tenant.domain.Membership;
import com.resto.tenant.service.UserService;
import lombok.Data;
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
}
