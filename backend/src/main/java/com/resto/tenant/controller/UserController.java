package com.resto.tenant.controller;

import com.resto.core.response.Response;
import com.resto.tenant.domain.User;
import com.resto.tenant.service.UserService;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public Response<User> createUser(@RequestBody CreateUserRequest request) {
        User user = userService.createUser(
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                request.getKeycloakId()
        );
        return Response.<User>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(user)
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER')")
    public Response<User> getUserById(@PathVariable("id") UUID id) {
        User user = userService.getUserById(id);
        return Response.<User>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(user)
                .build();
    }

    @Data
    public static class CreateUserRequest {
        private String email;
        private String firstName;
        private String lastName;
        private String keycloakId;
    }
}
