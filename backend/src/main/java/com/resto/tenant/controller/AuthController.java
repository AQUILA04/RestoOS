package com.resto.tenant.controller;

import com.resto.core.response.Response;
import com.resto.core.security.TenantContext;
import com.resto.tenant.service.AuthService;
import com.resto.tenant.service.InvitationService;
import com.resto.tenant.service.OidcCallbackService;
import com.resto.tenant.service.SignupService;
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
    private final SignupService signupService;
    private final OidcCallbackService oidcCallbackService;

    public AuthController(AuthService authService,
                          InvitationService invitationService,
                          SignupService signupService,
                          OidcCallbackService oidcCallbackService) {
        this.authService = authService;
        this.invitationService = invitationService;
        this.signupService = signupService;
        this.oidcCallbackService = oidcCallbackService;
    }

    @PostMapping("/signup")
    public Response<Map<String, Object>> signup(@RequestBody SignupRequest request) {
        Map<String, Object> result = signupService.signup(
                request.getOrganizationName(),
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPassword(),
                Boolean.TRUE.equals(request.getMultiStore())
        );
        return Response.<Map<String, Object>>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(result)
                .build();
    }

    @PostMapping("/oidc/callback")
    public Response<Map<String, Object>> oidcCallback(@RequestBody OidcCallbackRequest request) {
        Map<String, Object> result = oidcCallbackService.completeLogin(
                request.getCode(),
                request.getCodeVerifier(),
                request.getRedirectUri()
        );
        return Response.<Map<String, Object>>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(result)
                .build();
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

    public static class SignupRequest {
        private String organizationName;
        private String firstName;
        private String lastName;
        private String email;
        private String password;
        private Boolean multiStore;

        public String getOrganizationName() { return organizationName; }
        public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public Boolean getMultiStore() { return multiStore; }
        public void setMultiStore(Boolean multiStore) { this.multiStore = multiStore; }
    }

    public static class OidcCallbackRequest {
        private String code;
        private String codeVerifier;
        private String redirectUri;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getCodeVerifier() { return codeVerifier; }
        public void setCodeVerifier(String codeVerifier) { this.codeVerifier = codeVerifier; }
        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
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
