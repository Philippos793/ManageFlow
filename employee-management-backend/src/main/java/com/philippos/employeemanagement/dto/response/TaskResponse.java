package com.philippos.employeemanagement.dto.response;

import com.philippos.employeemanagement.entity.TaskStatus;

import java.time.Instant;
import java.util.List;

public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private Long employeeId;
    private String employeeName;
    private Long createdByUserId;
    private Integer timeAllowedMinutes;
    private TaskStatus status;
    private Integer progress;
    private boolean archived;
    private Instant createdAt;
    private Instant acceptedAt;
    private Instant deadline;
    private Instant completedAt;
    private List<TaskChecklistItemResponse> checklistItems;

    public TaskResponse() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }
    public Integer getTimeAllowedMinutes() { return timeAllowedMinutes; }
    public void setTimeAllowedMinutes(Integer timeAllowedMinutes) { this.timeAllowedMinutes = timeAllowedMinutes; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant acceptedAt) { this.acceptedAt = acceptedAt; }
    public Instant getDeadline() { return deadline; }
    public void setDeadline(Instant deadline) { this.deadline = deadline; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public List<TaskChecklistItemResponse> getChecklistItems() { return checklistItems; }
    public void setChecklistItems(List<TaskChecklistItemResponse> checklistItems) { this.checklistItems = checklistItems; }
}
