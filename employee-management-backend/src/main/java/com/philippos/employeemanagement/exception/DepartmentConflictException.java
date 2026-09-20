package com.philippos.employeemanagement.exception;

public class DepartmentConflictException extends RuntimeException {

    public DepartmentConflictException(String message) {
        super(message);
    }
}