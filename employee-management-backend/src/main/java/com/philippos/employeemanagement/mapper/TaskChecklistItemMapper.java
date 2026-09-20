package com.philippos.employeemanagement.mapper;

import com.philippos.employeemanagement.dto.request.TaskChecklistItemRequest;
import com.philippos.employeemanagement.dto.response.TaskChecklistItemResponse;
import com.philippos.employeemanagement.entity.Task;
import com.philippos.employeemanagement.entity.TaskChecklistItem;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TaskChecklistItemMapper {
    public TaskChecklistItem toEntity(TaskChecklistItemRequest request, Task task, Instant createdAt) {
        TaskChecklistItem item = new TaskChecklistItem();
        item.setTask(task);
        item.setDescription(request.getDescription().trim());
        item.setCompleted(false);
        item.setCreatedAt(createdAt);
        item.setCompletedAt(null);
        return item;
    }

    public TaskChecklistItemResponse toResponse(TaskChecklistItem item) {
        TaskChecklistItemResponse response = new TaskChecklistItemResponse();
        response.setId(item.getId());
        response.setDescription(item.getDescription());
        response.setCompleted(item.isCompleted());
        response.setCreatedAt(item.getCreatedAt());
        response.setCompletedAt(item.getCompletedAt());
        return response;
    }
}
