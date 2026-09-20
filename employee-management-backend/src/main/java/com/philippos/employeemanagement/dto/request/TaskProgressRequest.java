package com.philippos.employeemanagement.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class TaskProgressRequest {
    @NotNull(message = "Progress is required")
    @Min(value = 0, message = "Progress must be between 0 and 100")
    @Max(value = 100, message = "Progress must be between 0 and 100")
    private Integer progress;

    public TaskProgressRequest() {
    }

    public TaskProgressRequest(Integer progress) {
        this.progress = progress;
    }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
}
