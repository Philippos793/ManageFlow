package com.philippos.employeemanagement.mapper;

import com.philippos.employeemanagement.dto.response.NotificationResponse;
import com.philippos.employeemanagement.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setRecipientUserId(notification.getRecipientUser().getId());
        response.setTaskId(notification.getTask().getId());
        response.setType(notification.getType());
        response.setMessage(notification.getMessage());
        response.setCreatedAt(notification.getCreatedAt());
        response.setReadAt(notification.getReadAt());
        return response;
    }
}
