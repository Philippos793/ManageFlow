package com.philippos.employeemanagement.dto.request;

import com.philippos.employeemanagement.util.AccountValidation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AcceptEmployeeInvitationRequest {
    @NotBlank(message = "Invitation token is required")
    private String token;

    @NotBlank(message = "Username is required")
    @Size(min = AccountValidation.USERNAME_MIN_LENGTH,
            max = AccountValidation.USERNAME_MAX_LENGTH,
            message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = AccountValidation.USERNAME_PATTERN,
            message = "Username may contain only letters, numbers, periods, underscores, and hyphens")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = AccountValidation.PASSWORD_MIN_LENGTH,
            max = AccountValidation.PASSWORD_MAX_LENGTH,
            message = "Password must be between 8 and 72 characters")
    private String password;

    public AcceptEmployeeInvitationRequest() {}

    public AcceptEmployeeInvitationRequest(String token, String username, String password) {
        this.token = token;
        setUsername(username);
        this.password = password;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUsername() { return username; }
    public void setUsername(String username) {
        this.username = AccountValidation.normalizeUsername(username);
    }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
