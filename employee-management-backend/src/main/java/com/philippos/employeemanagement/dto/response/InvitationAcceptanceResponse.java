package com.philippos.employeemanagement.dto.response;

public class InvitationAcceptanceResponse {
    private final String message;
    private final String username;

    public InvitationAcceptanceResponse(String message, String username) {
        this.message = message;
        this.username = username;
    }

    public String getMessage() { return message; }
    public String getUsername() { return username; }
}
