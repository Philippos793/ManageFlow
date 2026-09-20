package com.philippos.employeemanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TaskChecklistItemRequest {
    @NotBlank(message = "Checklist item description is required")
    @Size(max = 500, message = "Checklist item description must not exceed 500 characters")
    private String description;

    public TaskChecklistItemRequest() {
    }

    public TaskChecklistItemRequest(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
