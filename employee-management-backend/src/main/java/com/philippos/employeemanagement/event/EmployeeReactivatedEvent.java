package com.philippos.employeemanagement.event;

public record EmployeeReactivatedEvent(String recipient, String employeeName) {
}
