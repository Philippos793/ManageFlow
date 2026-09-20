package com.philippos.employeemanagement.exception;

public class WorkShiftConflictException extends RuntimeException {
    public WorkShiftConflictException(String message) {
        super(message);
    }
}