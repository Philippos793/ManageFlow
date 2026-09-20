package com.philippos.employeemanagement.mapper;

import com.philippos.employeemanagement.dto.response.TaskActivityResponse;
import com.philippos.employeemanagement.entity.TaskActivity;
import org.springframework.stereotype.Component;

@Component
public class TaskActivityMapper {

    public TaskActivityResponse toResponse(TaskActivity activity) {
        TaskActivityResponse response = new TaskActivityResponse();
        response.setType(activity.getActivityType());
        response.setMessage(activity.getMessage());
        response.setActorUsername(activity.getActorUser().getUsername());
        response.setCreatedAt(activity.getCreatedAt());
        return response;
    }
}
