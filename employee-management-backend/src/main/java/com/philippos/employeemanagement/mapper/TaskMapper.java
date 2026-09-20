package com.philippos.employeemanagement.mapper;

import com.philippos.employeemanagement.dto.request.TaskCreateRequest;
import com.philippos.employeemanagement.dto.response.TaskResponse;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.Task;
import com.philippos.employeemanagement.entity.User;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TaskMapper {
    private final TaskChecklistItemMapper taskChecklistItemMapper;

    public TaskMapper(TaskChecklistItemMapper taskChecklistItemMapper) {
        this.taskChecklistItemMapper = taskChecklistItemMapper;
    }

    public Task toEntity(TaskCreateRequest request, Employee employee,
                         User createdBy, Instant createdAt) {
        Task task = new Task();
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        task.setAssignedEmployee(employee);
        task.setCreatedByUser(createdBy);
        task.setTimeAllowedMinutes(request.getTimeAllowedMinutes());
        task.setCreatedAt(createdAt);
        return task;
    }

    public TaskResponse toResponse(Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setEmployeeId(task.getAssignedEmployee().getId());
        response.setEmployeeName(task.getAssignedEmployee().getFirstName()
                + " " + task.getAssignedEmployee().getLastName());
        response.setCreatedByUserId(task.getCreatedByUser().getId());
        response.setTimeAllowedMinutes(task.getTimeAllowedMinutes());
        response.setStatus(task.getStatus());
        response.setProgress(task.getProgress());
        response.setArchived(task.isArchived());
        response.setCreatedAt(task.getCreatedAt());
        response.setAcceptedAt(task.getAcceptedAt());
        response.setDeadline(task.getDeadline());
        response.setCompletedAt(task.getCompletedAt());
        response.setChecklistItems(task.getChecklistItems().stream()
                .map(taskChecklistItemMapper::toResponse)
                .toList());
        return response;
    }
}
