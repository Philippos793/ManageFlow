package com.philippos.employeemanagement.exception;

public class RegistrationRequestConflictException extends RuntimeException {
    public RegistrationRequestConflictException(String message) {
        super(message);
    }
}
