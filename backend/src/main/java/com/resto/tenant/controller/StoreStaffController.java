package com.resto.tenant.controller;

import com.resto.core.response.Response;
import com.resto.tenant.domain.User;
import com.resto.tenant.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stores/{storeId}/staff")
public class StoreStaffController {

    private final UserService userService;

    public StoreStaffController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER', 'KITCHEN')")
    public Response<List<User>> listStaff(@PathVariable("storeId") UUID storeId) {
        return Response.<List<User>>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(userService.findUsersByStore(storeId))
                .build();
    }
}
