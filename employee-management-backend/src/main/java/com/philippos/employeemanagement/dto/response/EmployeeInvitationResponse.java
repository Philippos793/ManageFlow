package com.philippos.employeemanagement.dto.response;

import java.time.Instant;

public class EmployeeInvitationResponse {
    private final Long employeeId;
    private final String employeeName;
    private final Instant expiresAt;

    public EmployeeInvitationResponse(
            Long employeeId, String employeeName, Instant expiresAt) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.expiresAt = expiresAt;
    }

    public Long getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public Instant getExpiresAt() { return expiresAt; }
}
