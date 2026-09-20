package com.philippos.employeemanagement.dto.response;

public class EmployeeReactivationConflictResponse extends ApiErrorResponse {
    private final String code;
    private final Long employeeId;

    public EmployeeReactivationConflictResponse(
            int status, String error, String message, Long employeeId) {
        super(status, error, message);
        this.code = "INACTIVE_EMPLOYEE";
        this.employeeId = employeeId;
    }

    public String getCode() {
        return code;
    }

    public Long getEmployeeId() {
        return employeeId;
    }
}
