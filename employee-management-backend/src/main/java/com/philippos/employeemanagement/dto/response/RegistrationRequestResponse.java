package com.philippos.employeemanagement.dto.response;

import com.philippos.employeemanagement.entity.RegistrationStatus;

import java.time.LocalDateTime;

public class RegistrationRequestResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private RegistrationStatus status;
    private LocalDateTime createdAt;

    public RegistrationRequestResponse() {}
    public RegistrationRequestResponse(Long id, String firstName, String lastName, String email,
                                       String username, RegistrationStatus status, LocalDateTime createdAt) {
        this.id = id; this.firstName = firstName; this.lastName = lastName; this.email = email;
        this.username = username; this.status = status; this.createdAt = createdAt;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public RegistrationStatus getStatus() { return status; }
    public void setStatus(RegistrationStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
