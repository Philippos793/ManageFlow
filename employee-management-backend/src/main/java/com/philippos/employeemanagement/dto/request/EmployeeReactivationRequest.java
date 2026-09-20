package com.philippos.employeemanagement.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EmployeeReactivationRequest(
        @NotBlank(message = "First name is required") String firstName,
        @NotBlank(message = "Last name is required") String lastName) {
}
