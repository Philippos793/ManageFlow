package com.philippos.employeemanagement.dto.request;

import jakarta.validation.constraints.NotNull;

public class TaskChecklistItemUpdateRequest {
    @NotNull(message = "Completed is required")
    private Boolean completed;

    public TaskChecklistItemUpdateRequest() {
    }

    public TaskChecklistItemUpdateRequest(Boolean completed) {
        this.completed = completed;
    }

    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
}
