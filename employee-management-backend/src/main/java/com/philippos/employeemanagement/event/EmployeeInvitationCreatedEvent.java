package com.philippos.employeemanagement.event;

public record EmployeeInvitationCreatedEvent(
        String recipient,
        String employeeName,
        String token) {
}
