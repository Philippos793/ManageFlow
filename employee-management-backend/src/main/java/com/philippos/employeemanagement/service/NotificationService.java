package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.dto.response.NotificationResponse;
import com.philippos.employeemanagement.entity.Notification;
import com.philippos.employeemanagement.entity.NotificationType;
import com.philippos.employeemanagement.entity.Task;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.exception.NotificationNotFoundException;
import com.philippos.employeemanagement.mapper.NotificationMapper;
import com.philippos.employeemanagement.repository.NotificationRepository;
import com.philippos.employeemanagement.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;
    private final Clock clock;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               NotificationMapper notificationMapper,
                               Clock clock) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.notificationMapper = notificationMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications() {
        User user = getAuthenticatedUser();
        return notificationRepository
                .findAllByRecipientUser_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    public NotificationResponse markAsRead(Long id) {
        User user = getAuthenticatedUser();
        Notification notification = notificationRepository
                .findByIdAndRecipientUser_Id(id, user.getId())
                .orElseThrow(() -> new NotificationNotFoundException(
                        "Notification with id " + id + " not found"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(clock.instant());
            notificationRepository.save(notification);
        }
        return notificationMapper.toResponse(notification);
    }

    public Notification create(User recipient, Task task, NotificationType type) {
        Notification notification = new Notification();
        notification.setRecipientUser(recipient);
        notification.setTask(task);
        notification.setType(type);
        notification.setMessage(messageFor(type, task));
        notification.setCreatedAt(clock.instant());
        return notificationRepository.save(notification);
    }

    private String messageFor(NotificationType type, Task task) {
        if (type == NotificationType.TASK_UPDATED) {
            return "A task assigned to you has been updated.";
        }
        if (type == NotificationType.TASK_COMPLETED) {
            return "A task you assigned has been completed.";
        }
        if (type == NotificationType.TASK_CHANGES_REQUESTED) {
            return "Changes have been requested for your task.";
        }
        String action = switch (type) {
            case TASK_ASSIGNED -> "assigned";
            case TASK_ACCEPTED -> "accepted";
            case TASK_DECLINED -> "declined";
            case TASK_UPDATED -> throw new IllegalStateException("Handled above");
            case TASK_COMPLETED -> throw new IllegalStateException("Handled above");
            case TASK_CHANGES_REQUESTED -> throw new IllegalStateException("Handled above");
        };
        return "Task " + action + ": " + task.getTitle();
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authenticated user is required");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user was not found"));
    }
}
