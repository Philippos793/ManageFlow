package com.philippos.employeemanagement.event;

public record RegistrationApprovedEvent(String recipient, String username) {
}
