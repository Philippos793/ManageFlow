package com.philippos.employeemanagement.dto.request;

import com.philippos.employeemanagement.util.AccountValidation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistrationSubmissionRequest {
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "Email must contain a valid domain")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;
    @NotBlank(message = "Username is required")
    @Size(min = AccountValidation.USERNAME_MIN_LENGTH, max = AccountValidation.USERNAME_MAX_LENGTH,
            message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = AccountValidation.USERNAME_PATTERN,
            message = "Username may contain only letters, numbers, periods, underscores, and hyphens")
    private String username;
    @NotBlank(message = "Password is required")
    @Size(min = AccountValidation.PASSWORD_MIN_LENGTH, max = AccountValidation.PASSWORD_MAX_LENGTH,
            message = "Password must be between 8 and 72 characters")
    private String password;

    public RegistrationSubmissionRequest() {}
    public RegistrationSubmissionRequest(String firstName, String lastName, String email,
                                         String username, String password) {
        this.firstName = firstName; this.lastName = lastName; this.email = email;
        setUsername(username); this.password = password;
    }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) {
        this.username = AccountValidation.normalizeUsername(username);
    }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
