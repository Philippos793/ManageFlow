package com.philippos.employeemanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public class TaskCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    private String description;

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Time allowed is required")
    @Positive(message = "Time allowed must be greater than 0")
    private Integer timeAllowedMinutes;

    private List<@jakarta.validation.Valid TaskChecklistItemRequest> checklistItems;

    public TaskCreateRequest() {
    }

    public TaskCreateRequest(String title, String description, Long employeeId,
                             Integer timeAllowedMinutes) {
        this.title = title;
        this.description = description;
        this.employeeId = employeeId;
        this.timeAllowedMinutes = timeAllowedMinutes;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public Integer getTimeAllowedMinutes() { return timeAllowedMinutes; }
    public void setTimeAllowedMinutes(Integer timeAllowedMinutes) { this.timeAllowedMinutes = timeAllowedMinutes; }
    public List<TaskChecklistItemRequest> getChecklistItems() { return checklistItems; }
    public void setChecklistItems(List<TaskChecklistItemRequest> checklistItems) { this.checklistItems = checklistItems; }
}
