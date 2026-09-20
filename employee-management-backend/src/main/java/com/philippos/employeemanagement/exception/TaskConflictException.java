package com.philippos.employeemanagement.exception;

public class TaskConflictException extends RuntimeException {
    public TaskConflictException(String message) {
        super(message);
    }
}
