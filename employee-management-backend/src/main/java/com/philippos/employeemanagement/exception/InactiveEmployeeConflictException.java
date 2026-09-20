package com.philippos.employeemanagement.exception;

public class InactiveEmployeeConflictException extends RuntimeException {
    private final Long employeeId;

    public InactiveEmployeeConflictException(Long employeeId) {
        super("An inactive employee with this email already exists");
        this.employeeId = employeeId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }
}
