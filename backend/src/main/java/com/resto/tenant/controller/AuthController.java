package com.resto.tenant.controller;

import com.resto.core.response.Response;
import com.resto.core.security.TenantContext;
import com.resto.tenant.service.AuthService;
import com.resto.tenant.service.InvitationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final InvitationService invitationService;

    public AuthController(AuthService authService, InvitationService invitationService) {
        this.authService = authService;
        this.invitationService = invitationService;
    }

    @RequestMapping(value = "/activate", method = {RequestMethod.GET, RequestMethod.POST})
    public Response<Map<String, Object>> activate(@RequestParam("token") String token) {
        Map<String, Object> result = invitationService.activate(token);
        return Response.<Map<String, Object>>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(result)
                .build();
    }

    @PostMapping("/pin-login")
    public Response<Map<String, Object>> pinLogin(@RequestBody PinLoginRequest request) {
        Map<String, Object> result = authService.pinLogin(
                request.getUserId(),
                request.getStoreId() != null ? request.getStoreId() : TenantContext.getStoreId(),
                request.getOrganizationId() != null ? request.getOrganizationId() : TenantContext.getOrgId(),
                request.getPin()
        );
        boolean ok = Boolean.TRUE.equals(result.get("authenticated"));
        return Response.<Map<String, Object>>builder()
                .status(ok ? HttpStatus.OK : HttpStatus.UNAUTHORIZED)
                .statusCode(ok ? HttpStatus.OK.value() : HttpStatus.UNAUTHORIZED.value())
                .message(ok ? "default.message.success" : "error.invalid.pin")
                .service("RESTO-OS")
                .data(result)
                .build();
    }

    @PostMapping("/set-pin")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER')")
    public Response<String> setPin(@RequestBody SetPinRequest request) {
        authService.setPin(request.getUserId(), request.getPin());
        return Response.<String>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data("PIN successfully updated")
                .build();
    }

    public static class PinLoginRequest {
        private UUID userId;
        private UUID storeId;
        private UUID organizationId;
        private String pin;

        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        public UUID getStoreId() { return storeId; }
        public void setStoreId(UUID storeId) { this.storeId = storeId; }
        public UUID getOrganizationId() { return organizationId; }
        public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
        public String getPin() { return pin; }
        public void setPin(String pin) { this.pin = pin; }
    }

    public static class SetPinRequest {
        private UUID userId;
        private String pin;

        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        public String getPin() { return pin; }
        public void setPin(String pin) { this.pin = pin; }
    }
}
