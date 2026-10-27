package com.philippos.employeemanagement.controller;

import com.philippos.employeemanagement.dto.request.LoginRequest;
import com.philippos.employeemanagement.dto.response.LoginResponse;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.service.JwtService;
import com.philippos.employeemanagement.service.LoginAttemptService;
import com.philippos.employeemanagement.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    public AuthController(
            UserService userService,
            JwtService jwtService,
            LoginAttemptService loginAttemptService) {

        this.userService = userService;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request,
                               HttpServletRequest httpRequest) {
        String attemptKey = loginAttemptService.keyFor(
                request.getUsername(), httpRequest.getRemoteAddr());
        loginAttemptService.checkAllowed(attemptKey);

        User user;
        try {
            user = userService.login(
                    request.getUsername(),
                    request.getPassword()
            );
        } catch (BadCredentialsException exception) {
            loginAttemptService.recordFailure(attemptKey);
            throw exception;
        }
        loginAttemptService.recordSuccess(attemptKey);

        String token = jwtService.generateToken(user);

        return new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getRole()
        );
    }
}
