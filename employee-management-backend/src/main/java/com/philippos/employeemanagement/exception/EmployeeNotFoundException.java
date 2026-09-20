package com.philippos.employeemanagement.exception;

public class EmployeeNotFoundException extends RuntimeException {

  public EmployeeNotFoundException(String message) {

    // Save this message in RuntimeException class.
    super(message);
  }

}