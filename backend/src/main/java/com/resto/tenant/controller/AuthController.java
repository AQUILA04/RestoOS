package com.resto.tenant.controller;

import com.resto.core.response.Response;
import com.resto.tenant.service.AuthService;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/pin-login")
    public Response<Map<String, Object>> pinLogin(@RequestBody PinLoginRequest request) {
        boolean valid = authService.validatePin(request.getUserId(), request.getPin());
        if (!valid) {
            return Response.<Map<String, Object>>builder()
                    .status(HttpStatus.UNAUTHORIZED)
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .message("error.invalid.pin")
                    .service("RESTO-OS")
                    .data(Map.of("authenticated", false))
                    .build();
        }

        return Response.<Map<String, Object>>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(Map.of("authenticated", true, "userId", request.getUserId()))
                .build();
    }

    @PostMapping("/set-pin")
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

    @Data
    public static class PinLoginRequest {
        private UUID userId;
        private String pin;
    }

    @Data
    public static class SetPinRequest {
        private UUID userId;
        private String pin;
    }
}
