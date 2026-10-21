package com.udea.reservations.platform.users.controller;

import com.udea.reservations.platform.common.dto.ApiSuccessResponse;
import com.udea.reservations.platform.users.dto.UserRegistrationRequest;
import com.udea.reservations.platform.users.dto.UserResponse;
import com.udea.reservations.platform.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ApiSuccessResponse<UserResponse>> register(
        @Valid @RequestBody UserRegistrationRequest registrationRequest
    ) {
        UserResponse userResponse = userService.register(registrationRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(
            new ApiSuccessResponse<>(
                "Registro exitoso. Tu cuenta fue creada.",
                userResponse
            )
        );
    }
}
