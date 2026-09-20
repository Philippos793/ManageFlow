package com.philippos.employeemanagement.dto.request;

import com.philippos.employeemanagement.util.AccountValidation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Size(min = AccountValidation.USERNAME_MIN_LENGTH,
            max = AccountValidation.USERNAME_MAX_LENGTH,
            message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = AccountValidation.USERNAME_PATTERN,
            message = "Username may contain only letters, numbers, periods, underscores, and hyphens")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(max = AccountValidation.PASSWORD_MAX_LENGTH,
            message = "Password must not exceed 72 characters")
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String username, String password) {
        setUsername(username);
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = AccountValidation.normalizeUsername(username);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}