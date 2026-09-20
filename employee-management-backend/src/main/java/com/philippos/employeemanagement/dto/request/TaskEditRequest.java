package com.philippos.employeemanagement.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public class TaskEditRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    private String description;

    @NotNull(message = "Time allowed is required")
    @Positive(message = "Time allowed must be greater than 0")
    private Integer timeAllowedMinutes;

    private List<@Valid TaskChecklistItemEditRequest> checklistItems;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getTimeAllowedMinutes() { return timeAllowedMinutes; }
    public void setTimeAllowedMinutes(Integer timeAllowedMinutes) { this.timeAllowedMinutes = timeAllowedMinutes; }
    public List<TaskChecklistItemEditRequest> getChecklistItems() { return checklistItems; }
    public void setChecklistItems(List<TaskChecklistItemEditRequest> checklistItems) { this.checklistItems = checklistItems; }
}
