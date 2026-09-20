package com.philippos.employeemanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

import java.util.List;

public class TaskRequestChangesRequest {
    @NotBlank(message = "Feedback is required")
    private String feedback;

    @NotNull(message = "Additional time is required")
    @Min(value = 1, message = "Additional time must be greater than 0")
    private Integer additionalTimeMinutes;

    private List<@Valid TaskChecklistItemRequest> checklistItems;

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public Integer getAdditionalTimeMinutes() { return additionalTimeMinutes; }
    public void setAdditionalTimeMinutes(Integer additionalTimeMinutes) {
        this.additionalTimeMinutes = additionalTimeMinutes;
    }
    public List<TaskChecklistItemRequest> getChecklistItems() { return checklistItems; }
    public void setChecklistItems(List<TaskChecklistItemRequest> checklistItems) {
        this.checklistItems = checklistItems;
    }
}
