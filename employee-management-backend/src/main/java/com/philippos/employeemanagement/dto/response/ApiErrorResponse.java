package com.philippos.employeemanagement.dto.response;

import java.util.Map;

public class ApiErrorResponse {

    private int status;
    private String error;
    private String message;
    private Map<String, String> validationErrors;

    public ApiErrorResponse() {
    }

    public ApiErrorResponse(int status, String error, String message) {
        this(status, error, message, null);
    }

    public ApiErrorResponse(
            int status,
            String error,
            String message,
            Map<String, String> validationErrors) {

        this.status = status;
        this.error = error;
        this.message = message;
        this.validationErrors = validationErrors;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(Map<String, String> validationErrors) {
        this.validationErrors = validationErrors;
    }
}
