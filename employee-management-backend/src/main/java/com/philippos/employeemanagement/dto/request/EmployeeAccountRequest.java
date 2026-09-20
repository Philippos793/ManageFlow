package com.philippos.employeemanagement.dto.request;

import com.philippos.employeemanagement.util.AccountValidation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class EmployeeAccountRequest {
    @NotNull(message = "Employee ID is required")
    @Positive(message = "Employee ID must be positive")
    private Long employeeId;

    @NotBlank(message = "Username is required")
    @Size(min = AccountValidation.USERNAME_MIN_LENGTH,
            max = AccountValidation.USERNAME_MAX_LENGTH,
            message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = AccountValidation.USERNAME_PATTERN,
            message = "Username may contain only letters, numbers, periods, underscores, and hyphens")
    private String username;

    @NotBlank(message = "Temporary password is required")
    @Size(min = AccountValidation.PASSWORD_MIN_LENGTH,
            max = AccountValidation.PASSWORD_MAX_LENGTH,
            message = "Temporary password must be between 8 and 72 characters")
    private String temporaryPassword;

    public EmployeeAccountRequest() {}

    public EmployeeAccountRequest(Long employeeId, String username, String temporaryPassword) {
        this.employeeId = employeeId;
        setUsername(username);
        this.temporaryPassword = temporaryPassword;
    }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getUsername() { return username; }
    public void setUsername(String username) {
        this.username = AccountValidation.normalizeUsername(username);
    }
    public String getTemporaryPassword() { return temporaryPassword; }
    public void setTemporaryPassword(String temporaryPassword) { this.temporaryPassword = temporaryPassword; }
}