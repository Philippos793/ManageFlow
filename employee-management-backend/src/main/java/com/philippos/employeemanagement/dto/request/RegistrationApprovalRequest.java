package com.philippos.employeemanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class RegistrationApprovalRequest {
    @NotNull(message = "Department ID is required")
    @Positive(message = "Department ID must be positive")
    private Long departmentId;

    public RegistrationApprovalRequest() {}
    public RegistrationApprovalRequest(Long departmentId) { this.departmentId = departmentId; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
}
