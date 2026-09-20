package com.philippos.employeemanagement.dto.request;

import jakarta.validation.constraints.NotBlank;

public class InvitationTokenRequest {
    @NotBlank(message = "Invitation token is required")
    private String token;

    public InvitationTokenRequest() {}

    public InvitationTokenRequest(String token) {
        this.token = token;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
