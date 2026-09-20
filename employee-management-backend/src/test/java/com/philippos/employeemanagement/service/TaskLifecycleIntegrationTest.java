package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.dto.request.TaskCreateRequest;
import com.philippos.employeemanagement.dto.request.TaskProgressRequest;
import com.philippos.employeemanagement.dto.request.TaskRequestChangesRequest;
import com.philippos.employeemanagement.dto.response.TaskResponse;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.EmployeeStatus;
import com.philippos.employeemanagement.entity.Notification;
import com.philippos.employeemanagement.entity.NotificationType;
import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.Task;
import com.philippos.employeemanagement.entity.TaskActivity;
import com.philippos.employeemanagement.entity.TaskActivityType;
import com.philippos.employeemanagement.entity.TaskStatus;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.mapper.NotificationMapper;
import com.philippos.employeemanagement.mapper.TaskActivityMapper;
import com.philippos.employeemanagement.mapper.TaskChecklistItemMapper;
import com.philippos.employeemanagement.mapper.TaskMapper;
import com.philippos.employeemanagement.repository.EmployeeRepository;
import com.philippos.employeemanagement.repository.NotificationRepository;
import com.philippos.employeemanagement.repository.TaskActivityRepository;
import com.philippos.employeemanagement.repository.TaskChecklistItemRepository;
import com.philippos.employeemanagement.repository.TaskRepository;
import com.philippos.employeemanagement.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Exercises the production task, notification and activity services together.
 * Persistence is kept in memory because this project has no isolated database integration setup.
 */
@ExtendWith(MockitoExtension.class)
class TaskLifecycleIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-18T09:00:00Z");

    @Mock private TaskRepository taskRepository;
    @Mock private TaskChecklistItemRepository checklistItemRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private TaskActivityRepository activityRepository;

    private final Map<Long, Task> storedTasks = new HashMap<>();
    private final List<Notification> storedNotifications = new ArrayList<>();
    private final List<TaskActivity> storedActivities = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong(100);

    private TaskService taskService;
    private User admin;
    private User employeeUser;
    private Employee employee;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        TaskChecklistItemMapper checklistItemMapper = new TaskChecklistItemMapper();
        NotificationService notificationService = new NotificationService(
                notificationRepository, userRepository, new NotificationMapper(), clock);
        TaskActivityService activityService = new TaskActivityService(
                activityRepository, taskRepository, userRepository, new TaskActivityMapper(), clock);
        taskService = new TaskService(taskRepository, checklistItemRepository, employeeRepository,
                userRepository, new TaskMapper(checklistItemMapper), checklistItemMapper,
                notificationService, activityService, clock);

        admin = new User("admin", "encoded", Role.ADMIN);
        admin.setId(1L);
        employee = new Employee();
        employee.setId(7L);
        employee.setFirstName("Alex");
        employee.setLastName("Smith");
        employee.setEmail("alex.smith@example.com");
        employee.setStatus(EmployeeStatus.ACTIVE);
        employeeUser = new User("alex", "encoded", Role.EMPLOYEE);
        employeeUser.setId(2L);
        employeeUser.setEmployee(employee);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findByUsername("alex")).thenReturn(Optional.of(employeeUser));
        when(userRepository.findByEmployee_Id(employee.getId())).thenReturn(Optional.of(employeeUser));
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(taskRepository.findById(any(Long.class))).thenAnswer(invocation ->
                Optional.ofNullable(storedTasks.get(invocation.getArgument(0))));
        when(taskRepository.findAllByArchivedFalse()).thenAnswer(invocation -> storedTasks.values()
                .stream().filter(task -> !task.isArchived()).toList());
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            if (task.getId() == null) {
                task.setId(ids.getAndIncrement());
            }
            storedTasks.put(task.getId(), task);
            return task;
        });
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(ids.getAndIncrement());
            storedNotifications.add(notification);
            return notification;
        });
        when(activityRepository.save(any(TaskActivity.class))).thenAnswer(invocation -> {
            TaskActivity activity = invocation.getArgument(0);
            activity.setId(ids.getAndIncrement());
            storedActivities.add(activity);
            return activity;
        });
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void taskLifecycleKeepsHistoryAndCreatesNotificationsAtEachCoreTransition() {
        authenticate("admin");
        TaskResponse assigned = taskService.createTask(
                new TaskCreateRequest("Prepare monthly report", "Prepare the monthly report.", 7L, 180));
        Long taskId = assigned.getId();

        assertEquals(TaskStatus.PENDING, assigned.getStatus());
        assertNotification(NotificationType.TASK_ASSIGNED, employeeUser, taskId, 1);
        assertActivity(TaskActivityType.TASK_CREATED, admin, taskId, 1);

        authenticate("alex");
        TaskResponse accepted = taskService.acceptTask(taskId);
        assertEquals(TaskStatus.IN_PROGRESS, accepted.getStatus());
        assertEquals(NOW, accepted.getAcceptedAt());
        assertEquals(NOW.plusSeconds(180 * 60L), accepted.getDeadline());
        assertNotification(NotificationType.TASK_ACCEPTED, admin, taskId, 1);
        assertActivity(TaskActivityType.TASK_ACCEPTED, employeeUser, taskId, 1);

        TaskResponse firstCompletion = taskService.updateProgress(taskId, new TaskProgressRequest(100));
        assertEquals(TaskStatus.COMPLETED, firstCompletion.getStatus());
        assertEquals(100, firstCompletion.getProgress());
        assertEquals(NOW, firstCompletion.getCompletedAt());
        assertNotification(NotificationType.TASK_COMPLETED, admin, taskId, 1);
        assertActivity(TaskActivityType.PROGRESS_UPDATED, employeeUser, taskId, 1);
        assertActivity(TaskActivityType.TASK_COMPLETED, employeeUser, taskId, 1);

        authenticate("admin");
        TaskRequestChangesRequest changesRequest = new TaskRequestChangesRequest();
        changesRequest.setFeedback("Please add the final summary.");
        changesRequest.setAdditionalTimeMinutes(90);
        TaskResponse reopened = taskService.requestChanges(taskId, changesRequest);
        assertEquals(TaskStatus.IN_PROGRESS, reopened.getStatus());
        assertEquals(0, reopened.getProgress());
        assertNull(reopened.getCompletedAt());
        assertEquals(NOW.plusSeconds(90 * 60L), reopened.getDeadline());
        assertNotification(NotificationType.TASK_CHANGES_REQUESTED, employeeUser, taskId, 1);
        assertActivity(TaskActivityType.TASK_CHANGES_REQUESTED, admin, taskId, 1);

        authenticate("alex");
        TaskResponse secondCompletion = taskService.updateProgress(taskId, new TaskProgressRequest(100));
        assertEquals(TaskStatus.COMPLETED, secondCompletion.getStatus());
        assertEquals(NOW, secondCompletion.getCompletedAt());
        assertNotification(NotificationType.TASK_COMPLETED, admin, taskId, 2);
        assertActivity(TaskActivityType.TASK_COMPLETED, employeeUser, taskId, 2);

        authenticate("admin");
        TaskResponse archived = taskService.archiveTask(taskId);
        assertTrue(archived.isArchived());
        assertTrue(taskService.getTasks().isEmpty());
        assertActivity(TaskActivityType.TASK_ARCHIVED, admin, taskId, 1);

        assertNotNull(storedTasks.get(taskId));
        assertEquals(5, storedNotifications.size());
        assertEquals(8, storedActivities.size());
        assertFalse(storedNotifications.isEmpty());
        assertFalse(storedActivities.isEmpty());
    }

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of()));
    }

    private void assertNotification(NotificationType type, User recipient, Long taskId, long count) {
        List<Notification> matching = storedNotifications.stream()
                .filter(notification -> notification.getType() == type)
                .filter(notification -> notification.getRecipientUser().getId().equals(recipient.getId()))
                .filter(notification -> notification.getTask().getId().equals(taskId))
                .toList();
        assertEquals(count, matching.size());
    }

    private void assertActivity(TaskActivityType type, User actor, Long taskId, long count) {
        List<TaskActivity> matching = storedActivities.stream()
                .filter(activity -> activity.getActivityType() == type)
                .filter(activity -> activity.getActorUser().getId().equals(actor.getId()))
                .filter(activity -> activity.getTask().getId().equals(taskId))
                .toList();
        assertEquals(count, matching.size());
    }
}
