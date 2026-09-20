package com.philippos.employeemanagement.exception;

public class EmployeeAccountConflictException extends RuntimeException {
    public EmployeeAccountConflictException(String message) {
        super(message);
    }
}
