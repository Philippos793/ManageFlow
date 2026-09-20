package com.philippos.employeemanagement.exception;

public class EmployeeConflictException extends RuntimeException {
    public EmployeeConflictException(String message) {
        super(message);
    }
}