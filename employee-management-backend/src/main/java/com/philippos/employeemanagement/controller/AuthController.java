package com.philippos.employeemanagement.controller;

import com.philippos.employeemanagement.dto.request.LoginRequest;
import com.philippos.employeemanagement.dto.response.LoginResponse;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.service.JwtService;
import com.philippos.employeemanagement.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(
            UserService userService,
            JwtService jwtService) {

        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = userService.login(
                request.getUsername(),
                request.getPassword()
        );

        String token = jwtService.generateToken(user);

        return new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getRole()
        );
    }
}



