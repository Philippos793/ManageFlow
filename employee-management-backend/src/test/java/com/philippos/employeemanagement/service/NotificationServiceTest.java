package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.dto.response.NotificationResponse;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.Notification;
import com.philippos.employeemanagement.entity.NotificationType;
import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.Task;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.exception.NotificationNotFoundException;
import com.philippos.employeemanagement.mapper.NotificationMapper;
import com.philippos.employeemanagement.repository.NotificationRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-16T12:00:00Z");

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;

    private NotificationService notificationService;
    private User admin;
    private User employee;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, userRepository,
                new NotificationMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
        admin = new User("admin", "encoded", Role.ADMIN);
        admin.setId(1L);
        employee = new User("employee", "encoded", Role.EMPLOYEE);
        employee.setId(2L);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminSeesOnlyOwnNotifications() {
        authenticate("admin", admin);
        Notification notification = notification(10L, admin, null, NotificationType.TASK_ACCEPTED);
        when(notificationRepository.findAllByRecipientUser_IdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(notification));

        List<NotificationResponse> response = notificationService.getNotifications();

        assertEquals(List.of(10L), response.stream().map(NotificationResponse::getId).toList());
        verify(notificationRepository).findAllByRecipientUser_IdOrderByCreatedAtDesc(1L);
    }

    @Test
    void employeeSeesOnlyOwnNotifications() {
        authenticate("employee", employee);
        Notification notification = notification(11L, employee, null, NotificationType.TASK_ASSIGNED);
        when(notificationRepository.findAllByRecipientUser_IdOrderByCreatedAtDesc(2L))
                .thenReturn(List.of(notification));

        assertEquals(1, notificationService.getNotifications().size());
        verify(notificationRepository).findAllByRecipientUser_IdOrderByCreatedAtDesc(2L);
    }

    @Test
    void unreadNotificationBecomesReadWithClockTimestamp() {
        authenticate("employee", employee);
        Notification notification = notification(11L, employee, null, NotificationType.TASK_ASSIGNED);
        when(notificationRepository.findByIdAndRecipientUser_Id(11L, 2L))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationResponse response = notificationService.markAsRead(11L);

        assertEquals(NOW, response.getReadAt());
        verify(notificationRepository).save(notification);
    }

    @Test
    void alreadyReadNotificationKeepsItsReadTimestamp() {
        authenticate("employee", employee);
        Notification notification = notification(11L, employee, null, NotificationType.TASK_ASSIGNED);
        Instant readAt = NOW.minusSeconds(60);
        notification.setReadAt(readAt);
        when(notificationRepository.findByIdAndRecipientUser_Id(11L, 2L))
                .thenReturn(Optional.of(notification));

        assertEquals(readAt, notificationService.markAsRead(11L).getReadAt());
    }

    @Test
    void anotherUsersNotificationLooksNotFound() {
        authenticate("employee", employee);
        when(notificationRepository.findByIdAndRecipientUser_Id(11L, 2L))
                .thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class,
                () -> notificationService.markAsRead(11L));
    }

    @Test
    void createsNotificationForRecipientAndTask() {
        Task task = new Task();
        task.setId(15L);
        task.setTitle("Prepare report");
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = notificationService.create(
                employee, task, NotificationType.TASK_DECLINED);

        assertEquals(employee, notification.getRecipientUser());
        assertEquals(task, notification.getTask());
        assertEquals(NotificationType.TASK_DECLINED, notification.getType());
        assertEquals("Task declined: Prepare report", notification.getMessage());
        assertEquals(NOW, notification.getCreatedAt());
        assertNull(notification.getReadAt());
    }

    @Test
    void createsClearMessageForUpdatedTask() {
        Task task = new Task();
        task.setId(15L);
        task.setTitle("Prepare report");
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = notificationService.create(
                employee, task, NotificationType.TASK_UPDATED);

        assertEquals(NotificationType.TASK_UPDATED, notification.getType());
        assertEquals("A task assigned to you has been updated.", notification.getMessage());
    }

    @Test
    void createsClearMessageForCompletedTask() {
        Task task = new Task();
        task.setId(15L);
        task.setTitle("Prepare report");
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = notificationService.create(
                admin, task, NotificationType.TASK_COMPLETED);

        assertEquals(NotificationType.TASK_COMPLETED, notification.getType());
        assertEquals(admin, notification.getRecipientUser());
        assertEquals(task, notification.getTask());
        assertEquals("A task you assigned has been completed.", notification.getMessage());
    }

    @Test
    void createsClearMessageForRequestedChanges() {
        Task task = new Task();
        task.setId(15L);
        task.setTitle("Prepare report");
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = notificationService.create(
                employee, task, NotificationType.TASK_CHANGES_REQUESTED);

        assertEquals(NotificationType.TASK_CHANGES_REQUESTED, notification.getType());
        assertEquals("Changes have been requested for your task.", notification.getMessage());
    }

    private Notification notification(Long id, User recipient, Task task,
                                      NotificationType type) {
        if (task == null) {
            task = new Task();
            task.setId(15L);
            task.setTitle("Prepare report");
        }
        Notification notification = new Notification();
        notification.setId(id);
        notification.setRecipientUser(recipient);
        notification.setTask(task);
        notification.setType(type);
        notification.setMessage("Task notification");
        notification.setCreatedAt(NOW.minusSeconds(60));
        return notification;
    }

    private void authenticate(String username, User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of()));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    }
}
