package com.philippos.employeemanagement.dto.response;

import com.philippos.employeemanagement.entity.TaskActivityType;

import java.time.Instant;

public class TaskActivityResponse {
    private TaskActivityType type;
    private String message;
    private String actorUsername;
    private Instant createdAt;

    public TaskActivityType getType() { return type; }
    public void setType(TaskActivityType type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
