package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.dto.response.TaskActivityResponse;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.Task;
import com.philippos.employeemanagement.entity.TaskActivity;
import com.philippos.employeemanagement.entity.TaskActivityType;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.exception.EmployeeNotFoundException;
import com.philippos.employeemanagement.exception.TaskNotFoundException;
import com.philippos.employeemanagement.mapper.TaskActivityMapper;
import com.philippos.employeemanagement.repository.TaskActivityRepository;
import com.philippos.employeemanagement.repository.TaskRepository;
import com.philippos.employeemanagement.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
@Transactional
public class TaskActivityService {
    private final TaskActivityRepository taskActivityRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskActivityMapper taskActivityMapper;
    private final Clock clock;

    public TaskActivityService(TaskActivityRepository taskActivityRepository,
                               TaskRepository taskRepository,
                               UserRepository userRepository,
                               TaskActivityMapper taskActivityMapper,
                               Clock clock) {
        this.taskActivityRepository = taskActivityRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskActivityMapper = taskActivityMapper;
        this.clock = clock;
    }

    public void record(Task task, User actor, TaskActivityType type) {
        record(task, actor, type, messageFor(type));
    }

    public void record(Task task, User actor, TaskActivityType type, String message) {
        TaskActivity activity = new TaskActivity();
        activity.setTask(task);
        activity.setActorUser(actor);
        activity.setActivityType(type);
        activity.setMessage(message);
        activity.setCreatedAt(clock.instant());
        taskActivityRepository.save(activity);
    }

    @Transactional(readOnly = true)
    public List<TaskActivityResponse> getActivities(Long taskId) {
        User user = getAuthenticatedUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> taskNotFound(taskId));
        requireAccess(task, user);

        return taskActivityRepository.findAllByTask_IdOrderByCreatedAtAscIdAsc(taskId).stream()
                .map(taskActivityMapper::toResponse)
                .toList();
    }

    private void requireAccess(Task task, User user) {
        if (user.getRole() == Role.ADMIN && task.getCreatedByUser().getId().equals(user.getId())) {
            return;
        }
        if (user.getRole() == Role.EMPLOYEE
                && task.getAssignedEmployee().getId().equals(requireLinkedEmployee(user).getId())) {
            return;
        }
        throw taskNotFound(task.getId());
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authenticated user is required");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found"));
    }

    private Employee requireLinkedEmployee(User user) {
        if (user.getEmployee() == null) {
            throw new EmployeeNotFoundException(
                    "Authenticated employee does not have a linked employee record");
        }
        return user.getEmployee();
    }

    private TaskNotFoundException taskNotFound(Long taskId) {
        return new TaskNotFoundException("Task with id " + taskId + " not found");
    }

    private String messageFor(TaskActivityType type) {
        return switch (type) {
            case TASK_CREATED -> "Task created.";
            case TASK_ACCEPTED -> "Task accepted.";
            case TASK_DECLINED -> "Task declined.";
            case TASK_UPDATED -> "Task updated.";
            case CHECKLIST_UPDATED -> "Checklist updated.";
            case PROGRESS_UPDATED -> "Task progress updated.";
            case ATTACHMENT_UPLOADED -> "Attachment uploaded.";
            case TASK_COMPLETED -> "Task completed.";
            case TASK_ARCHIVED -> "Task archived.";
            case TASK_CHANGES_REQUESTED -> "Changes requested.";
        };
    }
}
